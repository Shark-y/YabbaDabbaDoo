package com.rts.datasource.ext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.io.IOTools;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.ServiceStatus.Status;
import com.cloud.core.w3.WebClient;
import com.rts.core.IBatchEventListener;
import com.rts.datasource.BasePollerDataSource;
import com.rts.datasource.DataFormat;
import com.rts.datasource.IDataSource;

/**
 * Prometheus API
----------------

Query metrics

http://192.168.40.84:32206/api/v1/label/__name__/values?_=1596217970606

Last Value Query
================
http://192.168.40.84:32206/api/v1/query?query=container_cpu_load_average_10s


Query Ranges
============
Get metric (container_file_descriptors)
http://192.168.40.84:32206/api/v1/query_range?query=container_file_descriptors&start=1596215065.211&end=1596218665.211&step=1000

(container_fs_inodes_free - last)
http://192.168.40.84:32206/api/v1/query_range?query=container_fs_inodes_free&start=1596215065.211&end=1596218665.211&step=10000

(container_cpu_load_average_10s)
http://192.168.40.84:32206/api/v1/query_range?query=container_cpu_load_average_10s&start=1596215065.211&end=1596218665.211&step=14
 * @author VSilva
 *
 */
public class PrometheusDataSource extends BasePollerDataSource implements IDataSource  {

	private static final Logger log = LogManager.getLogger(PrometheusDataSource.class);

	static void LOGD(final String text) {
		log.debug("[PROMETHEUS] " + text);
	}

	static void LOGE(final String text, Throwable t) {
		log.error("[PROMETHEUS] " + text, t);
	}

	static void LOGW(final String text) {
		log.warn("[PROMETHEUS] " + text);
	}

	/** Request variables */
	public static final String KEY_PM_URL 	= "pmUrl";
	public static final String KEY_PM_FREQ 	= "pmPollFreq";

	private final String url;			// poll url
	private final long frequency;		// poll freq (ms)
	
	/** Used to describe the {@link DataFormat} of this {@link IDataSource} */
	private DataFormat fmt;

	
	static class DashMetricTuple {
		String dash;
		String metric;
	
		public DashMetricTuple(String dash, String metric) {
			super();
			this.dash = dash;
			this.metric = metric;
		}
		
		@Override
		public String toString() {
			return "(" + dash  + "," + metric + ")";
		}
	}
	
	/**
	 * Event Handler
	 * @author VSilva
	 *
	 */
	private static class PollerEvents extends BasePollerDataSource.IPollEvents  {
		IBatchEventListener listener;
		
		private List<DashMetricTuple> metrics = new CopyOnWriteArrayList<DashMetricTuple>();

		private final WebClient wc;
		protected String baseUrl;
		protected String dsName;
		protected DataFormat fmt;
		private PrometheusDataSource parent;
		
		final Set<String> queryFields = new CopyOnWriteArraySet<String>();
		
		public PollerEvents( PrometheusDataSource parent ,final IBatchEventListener listener) {
			super( parent.frequency);
			this.listener = listener;
			this.wc = new WebClient();
			this.parent = parent;
			this.baseUrl = parent.url.endsWith( "/") ? parent.url.substring(0, parent.url.length() - 1) :  parent.url;
			this.dsName = parent.name; // dsName;
		}

		@Override
		public void fetch() throws Exception {
			final JSONObject root 	= new JSONObject();

			if ( metrics.size() == 0) {
				LOGD(dsName + " Fetch aborted. No metrics defined.");
				return;
			}
			
			// http://192.168.40.84:32206/api/v1/query?query=
			for (/*String*/ DashMetricTuple  tuple : metrics) {
				final JSONArray jbatch	= new JSONArray();
				final String ep 		= baseUrl + "/api/v1/query?query=" + tuple.metric;
				
				try {
					wc.setUrl(ep);
					
					/** {	"status": "success",
					"data": {
						"resultType": "matrix",
						"result": [{
							"metric": {
								"__name__": "container_cpu_load_average_10s",
								"beta_kubernetes_io_arch": "amd64",
								"beta_kubernetes_io_os": "linux",
								"container": "POD",
								"id": "/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod0ae21391_3913_4a2b_9453_d718f5d3992e.slice/docker-3260e4bd94cd0b6e4c0727c395f73645bd20c1b7d5bb1452a6c196bdac2c1979.scope",
								"image": "k8s.gcr.io/pause:3.2",
								"instance": "speedygonzales.cloudlab.com",
								"job": "kubernetes-nodes-cadvisor",
								"kubernetes_io_arch": "amd64",
								"kubernetes_io_hostname": "speedygonzales.cloudlab.com",
								"kubernetes_io_os": "linux",
								"name": "k8s_POD_rabbitmq-0_default_0ae21391-3913-4a2b-9453-d718f5d3992e_2",
								"namespace": "default",
								"pod": "rabbitmq-0"
							},
							"value": [1596215065.211, "0"]  }] } */
					JSONObject json 	= new JSONObject(wc.doGet());

					// check the status
					parent.checkStatus(parent.name, json);
					
					JSONArray result 	= json.getJSONObject("data").getJSONArray("result");
					
					LOGD(tuple + " results: " + result.length() + " from " + ep + " Freq: " + frequency);
					
					for (int i = 0; i < result.length(); i++) {
						// last result
						JSONObject last 	= result.getJSONObject(i); // .getJSONObject(result.length() - 1);
						JSONObject jmetric	= last.getJSONObject("metric");
						JSONObject jrecord 	= new JSONObject();
						
						// metric name
						String name 		= jmetric.getString("__name__");
						
						// Get the value [1596215065.211, "0"] = [DATE, VAL]
						JSONArray jvalue	= last.getJSONArray("value");
						//JSONArray jval		= jvalues.getJSONArray(jvalues.length() - 1);

						// Inject the metric date add the Unix date
						jmetric.put("__unix_date__", jvalue.get(0));
						addField("__unix_date__", i);
						
						// add meta data to the record
						for ( Object key : jmetric.keySet() ) {
							jrecord.put(key.toString(), jmetric.get(key.toString()));
							
							// add to @fields - key.toString()
							addField(key.toString(), i);
						}
						
						// add the value using the metric name as the key
						jrecord.put(name, jvalue.getString(1));
						addField(name, i);
						

						jbatch.put(jrecord);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				root.put("listenerName", dsName); 
				root.put("batchDate", System.currentTimeMillis());
				root.put("batchData", jbatch);
				root.put("dash", tuple.dash);
				
				// 9/21/2017 Add the format for micro services
				root.put("batchFormat", fmt.toJSON());
				
				listener.onBatchReceived(root);
			}
			
			/*
			 * {  "batchFormat": {
				  "recSep": "LF",
				  "fields": "aggregator_openapi_v2_regeneration_count,aggregator_openapi_v2_regeneration_duration,aggregator_unavailable_apiservice,apiserver_admission_controller_admission_duration_seconds_bucket,apiserver_admission_controller_admission_duration_seconds_count,apiserver_admission_controller_admission_duration_seconds_sum,apiserver_admission_step_admission_duration_seconds_bucket,apiserver_admission_step_admission_duration_seconds_count,apiserver_admission_step_admission_duration_seconds_sum,apiserver_admission_step_admission_duration_seconds_summary,apiserver_admission_step_admission_duration_seconds_summary_count,apiserver_admission_step_admission_duration_seconds_summary_sum,apiserver_audit_event_total,apiserver_audit_requests_rejected_total,apiserver_client_certificate_expiration_seconds_bucket,apiserver_client_certificate_expiration_seconds_count,apiserver_client_certificate_expiration_seconds_sum,apiserver_current_inflight_requests,apiserver_envelope_encryption_dek_cache_fill_percent,apiserver_init_events_total,apiserver_longrunning_gauge,apiserver_registered_watchers,apiserver_request_duration_seconds_bucket,apiserver_request_duration_seconds_count,apiserver_request_duration_seconds_sum,apiserver_request_terminations_total,apiserver_request_total,apiserver_response_sizes_bucket,apiserver_response_sizes_count,apiserver_response_sizes_sum,apiserver_storage_data_key_generation_duration_seconds_bucket,apiserver_storage_data_key_generation_duration_seconds_count,apiserver_storage_data_key_generation_duration_seconds_sum,apiserver_storage_data_key_generation_failures_total,apiserver_storage_envelope_transformation_cache_misses_total,apiserver_watch_events_sizes_bucket,apiserver_watch_events_sizes_count,apiserver_watch_events_sizes_sum,apiserver_watch_events_total,authenticated_user_requests,authentication_attempts,authentication_duration_seconds_bucket,authentication_duration_seconds_count,authentication_duration_seconds_sum,authentication_token_cache_active_fetch_count,authentication_token_cache_fetch_total,authentication_token_cache_request_duration_seconds_bucket,authentication_token_cache_request_duration_seconds_count,authentication_token_cache_request_duration_seconds_sum,authentication_token_cache_request_total,cadvisor_version_info,container_cpu_cfs_periods_total,container_cpu_cfs_throttled_periods_total,container_cpu_cfs_throttled_seconds_total,container_cpu_load_average_10s,container_cpu_system_seconds_total,container_cpu_usage_seconds_total,container_cpu_user_seconds_total,container_file_descriptors,container_fs_inodes_free,container_fs_inodes_total,container_fs_io_current,container_fs_io_time_seconds_total,container_fs_io_time_weighted_seconds_total,container_fs_limit_bytes,container_fs_read_seconds_total,container_fs_reads_bytes_total,container_fs_reads_merged_total,container_fs_reads_total,container_fs_sector_reads_total,container_fs_sector_writes_total,container_fs_usage_bytes,container_fs_write_seconds_total,container_fs_writes_bytes_total,container_fs_writes_merged_total,container_fs_writes_total,container_last_seen,container_memory_cache,container_memory_failcnt,container_memory_failures_total,container_memory_mapped_file,container_memory_max_usage_bytes,container_memory_rss,container_memory_swap,container_memory_usage_bytes,container_memory_working_set_bytes,container_network_receive_bytes_total,container_network_receive_errors_total,container_network_receive_packets_dropped_total,container_network_receive_packets_total,container_network_transmit_bytes_total,container_network_transmit_errors_total,container_network_transmit_packets_dropped_total,container_network_transmit_packets_total,container_processes,container_scrape_error,container_sockets,container_spec_cpu_period,container_spec_cpu_quota,container_spec_cpu_shares,container_spec_memory_limit_bytes,container_spec_memory_reservation_limit_bytes,container_spec_memory_swap_limit_bytes,container_start_time_seconds,container_tasks_state,container_threads,container_threads_max,coredns_build_info,coredns_cache_hits_total,coredns_cache_misses_total,coredns_cache_size,coredns_dns_request_count_total,coredns_dns_request_duration_seconds_bucket,coredns_dns_request_duration_seconds_count,coredns_dns_request_duration_seconds_sum,coredns_dns_request_size_bytes_bucket,coredns_dns_request_size_bytes_count,coredns_dns_request_size_bytes_sum,coredns_dns_request_type_count_total,coredns_dns_response_rcode_count_total,coredns_dns_response_size_bytes_bucket,coredns_dns_response_size_bytes_count,coredns_dns_response_size_bytes_sum,coredns_forward_request_count_total,coredns_forward_request_duration_seconds_bucket,coredns_forward_request_duration_seconds_count,coredns_forward_request_duration_seconds_sum,coredns_forward_response_rcode_count_total,coredns_health_request_duration_seconds_bucket,coredns_health_request_duration_seconds_count,coredns_health_request_duration_seconds_sum,coredns_kubernetes_dns_programming_duration_seconds_bucket,coredns_kubernetes_dns_programming_duration_seconds_count,coredns_kubernetes_dns_programming_duration_seconds_sum,coredns_panic_count_total,coredns_plugin_enabled,etcd_object_counts,etcd_request_duration_seconds_bucket,etcd_request_duration_seconds_count,etcd_request_duration_seconds_sum,get_token_count,get_token_fail_count,go_gc_duration_seconds,go_gc_duration_seconds_count,go_gc_duration_seconds_sum,go_goroutines,go_info,go_memstats_alloc_bytes,go_memstats_alloc_bytes_total,go_memstats_buck_hash_sys_bytes,go_memstats_frees_total,go_memstats_gc_cpu_fraction,go_memstats_gc_sys_bytes,go_memstats_heap_alloc_bytes,go_memstats_heap_idle_bytes,go_memstats_heap_inuse_bytes,go_memstats_heap_objects,go_memstats_heap_released_bytes,go_memstats_heap_sys_bytes,go_memstats_last_gc_time_seconds,go_memstats_lookups_total,go_memstats_mallocs_total,go_memstats_mcache_inuse_bytes,go_memstats_mcache_sys_bytes,go_memstats_mspan_inuse_bytes,go_memstats_mspan_sys_bytes,go_memstats_next_gc_bytes,go_memstats_other_sys_bytes,go_memstats_stack_inuse_bytes,go_memstats_stack_sys_bytes,go_memstats_sys_bytes,go_threads,grpc_client_handled_total,grpc_client_msg_received_total,grpc_client_msg_sent_total,grpc_client_started_total,kube_configmap_created,kube_configmap_info,kube_configmap_metadata_resource_version,kube_cronjob_created,kube_cronjob_info,kube_cronjob_labels,kube_cronjob_next_schedule_time,kube_cronjob_spec_suspend,kube_cronjob_status_active,kube_cronjob_status_last_schedule_time,kube_daemonset_created,kube_daemonset_labels,kube_daemonset_metadata_generation,kube_daemonset_status_current_number_scheduled,kube_daemonset_status_desired_number_scheduled,kube_daemonset_status_number_available,kube_daemonset_status_number_misscheduled,kube_daemonset_status_number_ready,kube_daemonset_status_number_unavailable,kube_daemonset_updated_number_scheduled,kube_deployment_created,kube_deployment_labels,kube_deployment_metadata_generation,kube_deployment_spec_paused,kube_deployment_spec_replicas,kube_deployment_spec_strategy_rollingupdate_max_surge,kube_deployment_spec_strategy_rollingupdate_max_unavailable,kube_deployment_status_condition,kube_deployment_status_observed_generation,kube_deployment_status_replicas,kube_deployment_status_replicas_available,kube_deployment_status_replicas_unavailable,kube_deployment_status_replicas_updated,kube_endpoint_address_available,kube_endpoint_address_not_ready,kube_endpoint_created,kube_endpoint_info,kube_endpoint_labels,kube_ingress_created,kube_ingress_info,kube_ingress_labels,kube_ingress_metadata_resource_version,kube_ingress_path,kube_job_complete,kube_job_created,kube_job_info,kube_job_labels,kube_job_owner,kube_job_spec_completions,kube_job_spec_parallelism,kube_job_status_active,kube_job_status_completion_time,kube_job_status_failed,kube_job_status_start_time,kube_job_status_succeeded,kube_namespace_created,kube_namespace_labels,kube_namespace_status_phase,kube_node_created,kube_node_info,kube_node_labels,kube_node_role,kube_node_spec_taint,kube_node_spec_unschedulable,kube_node_status_allocatable,kube_node_status_allocatable_cpu_cores,kube_node_status_allocatable_memory_bytes,kube_node_status_allocatable_pods,kube_node_status_capacity,kube_node_status_capacity_cpu_cores,kube_node_status_capacity_memory_bytes,kube_node_status_capacity_pods,kube_node_status_condition,kube_persistentvolume_capacity_bytes,kube_persistentvolume_info,kube_persistentvolume_labels,kube_persistentvolume_status_phase,kube_persistentvolumeclaim_access_mode,kube_persistentvolumeclaim_info,kube_persistentvolumeclaim_labels,kube_persistentvolumeclaim_resource_requests_storage_bytes,kube_persistentvolumeclaim_status_phase,kube_pod_completion_time,kube_pod_container_info,kube_pod_container_resource_limits,kube_pod_container_resource_limits_cpu_cores,kube_pod_container_resource_limits_memory_bytes,kube_pod_container_resource_requests,kube_pod_container_resource_requests_cpu_cores,kube_pod_container_resource_requests_memory_bytes,kube_pod_container_status_last_terminated_reason,kube_pod_container_status_ready,kube_pod_container_status_restarts_total,kube_pod_container_status_running,kube_pod_container_status_terminated,kube_pod_container_status_terminated_reason,kube_pod_container_status_waiting,kube_pod_container_status_waiting_reason,kube_pod_created,kube_pod_info,kube_pod_init_container_info,kube_pod_init_container_status_last_terminated_reason,kube_pod_init_container_status_ready,kube_pod_init_container_status_restarts_total,kube_pod_init_container_status_running,kube_pod_init_container_status_terminated,kube_pod_init_container_status_terminated_reason,kube_pod_init_container_status_waiting,kube_pod_init_container_status_waiting_reason,kube_pod_labels,kube_pod_owner,kube_pod_restart_policy,kube_pod_spec_volumes_persistentvolumeclaims_info,kube_pod_spec_volumes_persistentvolumeclaims_readonly,kube_pod_start_time,kube_pod_status_phase,kube_pod_status_ready,kube_pod_status_scheduled,kube_pod_status_scheduled_time,kube_replicaset_created,kube_replicaset_labels,kube_replicaset_metadata_generation,kube_replicaset_owner,kube_replicaset_spec_replicas,kube_replicaset_status_fully_labeled_replicas,kube_replicaset_status_observed_generation,kube_replicaset_status_ready_replicas,kube_replicaset_status_replicas,kube_secret_created,kube_secret_info,kube_secret_labels,kube_secret_metadata_resource_version,kube_secret_type,kube_service_created,kube_service_info,kube_service_labels,kube_service_spec_type,kube_statefulset_created,kube_statefulset_labels,kube_statefulset_metadata_generation,kube_statefulset_replicas,kube_statefulset_status_current_revision,kube_statefulset_status_observed_generation,kube_statefulset_status_replicas,kube_statefulset_status_replicas_current,kube_statefulset_status_replicas_ready,kube_statefulset_status_replicas_updated,kube_statefulset_status_update_revision,kubelet_certificate_manager_client_expiration_renew_errors,kubelet_certificate_manager_client_expiration_seconds,kubelet_cgroup_manager_duration_seconds_bucket,kubelet_cgroup_manager_duration_seconds_count,kubelet_cgroup_manager_duration_seconds_sum,kubelet_container_log_filesystem_used_bytes,kubelet_containers_per_pod_count_bucket,kubelet_containers_per_pod_count_count,kubelet_containers_per_pod_count_sum,kubelet_docker_operations_duration_seconds_bucket,kubelet_docker_operations_duration_seconds_count,kubelet_docker_operations_duration_seconds_sum,kubelet_docker_operations_errors_total,kubelet_docker_operations_total,kubelet_http_inflight_requests,kubelet_http_requests_duration_seconds_bucket,kubelet_http_requests_duration_seconds_count,kubelet_http_requests_duration_seconds_sum,kubelet_http_requests_total,kubelet_network_plugin_operations_duration_seconds_bucket,kubelet_network_plugin_operations_duration_seconds_count,kubelet_network_plugin_operations_duration_seconds_sum,kubelet_node_config_error,kubelet_node_name,kubelet_pleg_discard_events,kubelet_pleg_last_seen_seconds,kubelet_pleg_relist_duration_seconds_bucket,kubelet_pleg_relist_duration_seconds_count,kubelet_pleg_relist_duration_seconds_sum,kubelet_pleg_relist_interval_seconds_bucket,kubelet_pleg_relist_interval_seconds_count,kubelet_pleg_relist_interval_seconds_sum,kubelet_pod_start_duration_seconds_bucket,kubelet_pod_start_duration_seconds_count,kubelet_pod_start_duration_seconds_sum,kubelet_pod_worker_duration_seconds_bucket,kubelet_pod_worker_duration_seconds_count,kubelet_pod_worker_duration_seconds_sum,kubelet_pod_worker_start_duration_seconds_bucket,kubelet_pod_worker_start_duration_seconds_count,kubelet_pod_worker_start_duration_seconds_sum,kubelet_run_podsandbox_duration_seconds_bucket,kubelet_run_podsandbox_duration_seconds_count,kubelet_run_podsandbox_duration_seconds_sum,kubelet_run_podsandbox_errors_total,kubelet_running_container_count,kubelet_running_pod_count,kubelet_runtime_operations_duration_seconds_bucket,kubelet_runtime_operations_duration_seconds_count,kubelet_runtime_operations_duration_seconds_sum,kubelet_runtime_operations_errors_total,kubelet_runtime_operations_total,kubelet_volume_stats_available_bytes,kubelet_volume_stats_capacity_bytes,kubelet_volume_stats_inodes,kubelet_volume_stats_inodes_free,kubelet_volume_stats_inodes_used,kubelet_volume_stats_used_bytes,kubernetes_build_info,machine_cpu_cores,machine_memory_bytes,net_conntrack_dialer_conn_attempted_total,net_conntrack_dialer_conn_closed_total,net_conntrack_dialer_conn_established_total,net_conntrack_dialer_conn_failed_total,net_conntrack_listener_conn_accepted_total,net_conntrack_listener_conn_closed_total,node_arp_entries,node_boot_time_seconds,node_context_switches_total,node_cpu_guest_seconds_total,node_cpu_seconds_total,node_disk_io_now,node_disk_io_time_seconds_total,node_disk_io_time_weighted_seconds_total,node_disk_read_bytes_total,node_disk_read_time_seconds_total,node_disk_reads_completed_total,node_disk_reads_merged_total,node_disk_write_time_seconds_total,node_disk_writes_completed_total,node_disk_writes_merged_total,node_disk_written_bytes_total,node_entropy_available_bits,node_exporter_build_info,node_filefd_allocated,node_filefd_maximum,node_filesystem_avail_bytes,node_filesystem_device_error,node_filesystem_files,node_filesystem_files_free,node_filesystem_free_bytes,node_filesystem_readonly,node_filesystem_size_bytes,node_forks_total,node_hwmon_chip_names,node_hwmon_sensor_label,node_hwmon_temp_celsius,node_hwmon_temp_crit_alarm_celsius,node_hwmon_temp_crit_celsius,node_hwmon_temp_max_celsius,node_intr_total,node_ipvs_connections_total,node_ipvs_incoming_bytes_total,node_ipvs_incoming_packets_total,node_ipvs_outgoing_bytes_total,node_ipvs_outgoing_packets_total,node_load1,node_load15,node_load5,node_memory_Active_anon_bytes,node_memory_Active_bytes,node_memory_Active_file_bytes,node_memory_AnonHugePages_bytes,node_memory_AnonPages_bytes,node_memory_Bounce_bytes,node_memory_Buffers_bytes,node_memory_Cached_bytes,node_memory_CmaFree_bytes,node_memory_CmaTotal_bytes,node_memory_CommitLimit_bytes,node_memory_Committed_AS_bytes,node_memory_DirectMap2M_bytes,node_memory_DirectMap4k_bytes,node_memory_Dirty_bytes,node_memory_HardwareCorrupted_bytes,node_memory_HugePages_Free,node_memory_HugePages_Rsvd,node_memory_HugePages_Surp,node_memory_HugePages_Total,node_memory_Hugepagesize_bytes,node_memory_Inactive_anon_bytes,node_memory_Inactive_bytes,node_memory_Inactive_file_bytes,node_memory_KernelStack_bytes,node_memory_Mapped_bytes,node_memory_MemAvailable_bytes,node_memory_MemFree_bytes,node_memory_MemTotal_bytes,node_memory_Mlocked_bytes,node_memory_NFS_Unstable_bytes,node_memory_PageTables_bytes,node_memory_SReclaimable_bytes,node_memory_SUnreclaim_bytes,node_memory_Shmem_bytes,node_memory_Slab_bytes,node_memory_SwapCached_bytes,node_memory_SwapFree_bytes,node_memory_SwapTotal_bytes,node_memory_Unevictable_bytes,node_memory_VmallocChunk_bytes,node_memory_VmallocTotal_bytes,node_memory_VmallocUsed_bytes,node_memory_WritebackTmp_bytes,node_memory_Writeback_bytes,node_netstat_Icmp6_InErrors,node_netstat_Icmp6_InMsgs,node_netstat_Icmp6_OutMsgs,node_netstat_Icmp_InErrors,node_netstat_Icmp_InMsgs,node_netstat_Icmp_OutMsgs,node_netstat_Ip6_InOctets,node_netstat_Ip6_OutOctets,node_netstat_IpExt_InOctets,node_netstat_IpExt_OutOctets,node_netstat_Ip_Forwarding,node_netstat_TcpExt_ListenDrops,node_netstat_TcpExt_ListenOverflows,node_netstat_TcpExt_SyncookiesFailed,node_netstat_TcpExt_SyncookiesRecv,node_netstat_TcpExt_SyncookiesSent,node_netstat_TcpExt_TCPSynRetrans,node_netstat_Tcp_ActiveOpens,node_netstat_Tcp_CurrEstab,node_netstat_Tcp_InErrs,node_netstat_Tcp_InSegs,node_netstat_Tcp_OutSegs,node_netstat_Tcp_PassiveOpens,node_netstat_Tcp_RetransSegs,node_netstat_Udp6_InDatagrams,node_netstat_Udp6_InErrors,node_netstat_Udp6_NoPorts,node_netstat_Udp6_OutDatagrams,node_netstat_UdpLite6_InErrors,node_netstat_UdpLite_InErrors,node_netstat_Udp_InDatagrams,node_netstat_Udp_InErrors,node_netstat_Udp_NoPorts,node_netstat_Udp_OutDatagrams,node_network_address_assign_type,node_network_carrier,node_network_carrier_changes_total,node_network_device_id,node_network_dormant,node_network_flags,node_network_iface_id,node_network_iface_link,node_network_iface_link_mode,node_network_info,node_network_mtu_bytes,node_network_net_dev_group,node_network_protocol_type,node_network_receive_bytes_total,node_network_receive_compressed_total,node_network_receive_drop_total,node_network_receive_errs_total,node_network_receive_fifo_total,node_network_receive_frame_total,node_network_receive_multicast_total,node_network_receive_packets_total,node_network_speed_bytes,node_network_transmit_bytes_total,node_network_transmit_carrier_total,node_network_transmit_colls_total,node_network_transmit_compressed_total,node_network_transmit_drop_total,node_network_transmit_errs_total,node_network_transmit_fifo_total,node_network_transmit_packets_total,node_network_transmit_queue_length,node_network_up,node_nf_conntrack_entries,node_nf_conntrack_entries_limit,node_nfs_connections_total,node_nfs_packets_total,node_nfs_requests_total,node_nfs_rpc_authentication_refreshes_total,node_nfs_rpc_retransmissions_total,node_nfs_rpcs_total,node_procs_blocked,node_procs_running,node_scrape_collector_duration_seconds,node_scrape_collector_success,node_sockstat_FRAG_inuse,node_sockstat_FRAG_memory,node_sockstat_RAW_inuse,node_sockstat_TCP_alloc,node_sockstat_TCP_inuse,node_sockstat_TCP_mem,node_sockstat_TCP_mem_bytes,node_sockstat_TCP_orphan,node_sockstat_TCP_tw,node_sockstat_UDPLITE_inuse,node_sockstat_UDP_inuse,node_sockstat_UDP_mem,node_sockstat_UDP_mem_bytes,node_sockstat_sockets_used,node_textfile_scrape_error,node_time_seconds,node_timex_estimated_error_seconds,node_timex_frequency_adjustment_ratio,node_timex_loop_time_constant,node_timex_maxerror_seconds,node_timex_offset_seconds,node_timex_pps_calibration_total,node_timex_pps_error_total,node_timex_pps_frequency_hertz,node_timex_pps_jitter_seconds,node_timex_pps_jitter_total,node_timex_pps_shift_seconds,node_timex_pps_stability_exceeded_total,node_timex_pps_stability_hertz,node_timex_status,node_timex_sync_status,node_timex_tai_offset_seconds,node_timex_tick_seconds,node_uname_info,node_vmstat_pgfault,node_vmstat_pgmajfault,node_vmstat_pgpgin,node_vmstat_pgpgout,node_vmstat_pswpin,node_vmstat_pswpout,node_xfs_allocation_btree_compares_total,node_xfs_allocation_btree_lookups_total,node_xfs_allocation_btree_records_deleted_total,node_xfs_allocation_btree_records_inserted_total,node_xfs_block_map_btree_compares_total,node_xfs_block_map_btree_lookups_total,node_xfs_block_map_btree_records_deleted_total,node_xfs_block_map_btree_records_inserted_total,node_xfs_block_mapping_extent_list_compares_total,node_xfs_block_mapping_extent_list_deletions_total,node_xfs_block_mapping_extent_list_insertions_total,node_xfs_block_mapping_extent_list_lookups_total,node_xfs_block_mapping_reads_total,node_xfs_block_mapping_unmaps_total,node_xfs_block_mapping_writes_total,node_xfs_extent_allocation_blocks_allocated_total,node_xfs_extent_allocation_blocks_freed_total,node_xfs_extent_allocation_extents_allocated_total,node_xfs_extent_allocation_extents_freed_total,process_cpu_seconds_total,process_max_fds,process_open_fds,process_resident_memory_bytes,process_start_time_seconds,process_virtual_memory_bytes,process_virtual_memory_max_bytes,prometheus_api_remote_read_queries,prometheus_build_info,prometheus_config_last_reload_success_timestamp_seconds,prometheus_config_last_reload_successful,prometheus_engine_queries,prometheus_engine_queries_concurrent_max,prometheus_engine_query_duration_seconds,prometheus_engine_query_duration_seconds_count,prometheus_engine_query_duration_seconds_sum,prometheus_engine_query_log_enabled,prometheus_engine_query_log_failures_total,prometheus_http_request_duration_seconds_bucket,prometheus_http_request_duration_seconds_count,prometheus_http_request_duration_seconds_sum,prometheus_http_requests_total,prometheus_http_response_size_bytes_bucket,prometheus_http_response_size_bytes_count,prometheus_http_response_size_bytes_sum,prometheus_notifications_alertmanagers_discovered,prometheus_notifications_dropped_total,prometheus_notifications_errors_total,prometheus_notifications_queue_capacity,prometheus_notifications_queue_length,prometheus_notifications_sent_total,prometheus_remote_storage_highest_timestamp_in_seconds,prometheus_remote_storage_samples_in_total,prometheus_remote_storage_string_interner_zero_reference_releases_total,prometheus_rule_evaluation_duration_seconds,prometheus_rule_evaluation_duration_seconds_count,prometheus_rule_evaluation_duration_seconds_sum,prometheus_rule_group_duration_seconds,prometheus_rule_group_duration_seconds_count,prometheus_rule_group_duration_seconds_sum,prometheus_rule_group_iterations_missed_total,prometheus_rule_group_iterations_total,prometheus_sd_consul_rpc_duration_seconds,prometheus_sd_consul_rpc_duration_seconds_count,prometheus_sd_consul_rpc_duration_seconds_sum,prometheus_sd_consul_rpc_failures_total,prometheus_sd_discovered_targets,prometheus_sd_dns_lookup_failures_total,prometheus_sd_dns_lookups_total,prometheus_sd_failed_configs,prometheus_sd_file_read_errors_total,prometheus_sd_file_scan_duration_seconds,prometheus_sd_file_scan_duration_seconds_count,prometheus_sd_file_scan_duration_seconds_sum,prometheus_sd_kubernetes_events_total,prometheus_sd_kubernetes_http_request_duration_seconds_count,prometheus_sd_kubernetes_http_request_duration_seconds_sum,prometheus_sd_kubernetes_http_request_total,prometheus_sd_kubernetes_workqueue_depth,prometheus_sd_kubernetes_workqueue_items_total,prometheus_sd_kubernetes_workqueue_latency_seconds_count,prometheus_sd_kubernetes_workqueue_latency_seconds_sum,prometheus_sd_kubernetes_workqueue_longest_running_processor_seconds,prometheus_sd_kubernetes_workqueue_unfinished_work_seconds,prometheus_sd_kubernetes_workqueue_work_duration_seconds_count,prometheus_sd_kubernetes_workqueue_work_duration_seconds_sum,prometheus_sd_received_updates_total,prometheus_sd_updates_total,prometheus_target_interval_length_seconds,prometheus_target_interval_length_seconds_count,prometheus_target_interval_length_seconds_sum,prometheus_target_metadata_cache_bytes,prometheus_target_metadata_cache_entries,prometheus_target_scrape_pool_reloads_failed_total,prometheus_target_scrape_pool_reloads_total,prometheus_target_scrape_pool_sync_total,prometheus_target_scrape_pools_failed_total,prometheus_target_scrape_pools_total,prometheus_target_scrapes_cache_flush_forced_total,prometheus_target_scrapes_exceeded_sample_limit_total,prometheus_target_scrapes_sample_duplicate_timestamp_total,prometheus_target_scrapes_sample_out_of_bounds_total,prometheus_target_scrapes_sample_out_of_order_total,prometheus_target_sync_length_seconds,prometheus_target_sync_length_seconds_count,prometheus_target_sync_length_seconds_sum,prometheus_template_text_expansion_failures_total,prometheus_template_text_expansions_total,prometheus_treecache_watcher_goroutines,prometheus_treecache_zookeeper_failures_total,prometheus_tsdb_blocks_loaded,prometheus_tsdb_checkpoint_creations_failed_total,prometheus_tsdb_checkpoint_creations_total,prometheus_tsdb_checkpoint_deletions_failed_total,prometheus_tsdb_checkpoint_deletions_total,prometheus_tsdb_compaction_chunk_range_seconds_bucket,prometheus_tsdb_compaction_chunk_range_seconds_count,prometheus_tsdb_compaction_chunk_range_seconds_sum,prometheus_tsdb_compaction_chunk_samples_bucket,prometheus_tsdb_compaction_chunk_samples_count,prometheus_tsdb_compaction_chunk_samples_sum,prometheus_tsdb_compaction_chunk_size_bytes_bucket,prometheus_tsdb_compaction_chunk_size_bytes_count,prometheus_tsdb_compaction_chunk_size_bytes_sum,prometheus_tsdb_compaction_duration_seconds_bucket,prometheus_tsdb_compaction_duration_seconds_count,prometheus_tsdb_compaction_duration_seconds_sum,prometheus_tsdb_compaction_populating_block,prometheus_tsdb_compactions_failed_total,prometheus_tsdb_compactions_skipped_total,prometheus_tsdb_compactions_total,prometheus_tsdb_compactions_triggered_total,prometheus_tsdb_head_active_appenders,prometheus_tsdb_head_chunks,prometheus_tsdb_head_chunks_created_total,prometheus_tsdb_head_chunks_removed_total,prometheus_tsdb_head_gc_duration_seconds_count,prometheus_tsdb_head_gc_duration_seconds_sum,prometheus_tsdb_head_max_time,prometheus_tsdb_head_max_time_seconds,prometheus_tsdb_head_min_time,prometheus_tsdb_head_min_time_seconds,prometheus_tsdb_head_samples_appended_total,prometheus_tsdb_head_series,prometheus_tsdb_head_series_created_total,prometheus_tsdb_head_series_not_found_total,prometheus_tsdb_head_series_removed_total,prometheus_tsdb_head_truncations_failed_total,prometheus_tsdb_head_truncations_total,prometheus_tsdb_isolation_high_watermark,prometheus_tsdb_isolation_low_watermark,prometheus_tsdb_lowest_timestamp,prometheus_tsdb_lowest_timestamp_seconds,prometheus_tsdb_reloads_failures_total,prometheus_tsdb_reloads_total,prometheus_tsdb_retention_limit_bytes,prometheus_tsdb_size_retentions_total,prometheus_tsdb_storage_blocks_bytes,prometheus_tsdb_symbol_table_size_bytes,prometheus_tsdb_time_retentions_total,prometheus_tsdb_tombstone_cleanup_seconds_bucket,prometheus_tsdb_tombstone_cleanup_seconds_count,prometheus_tsdb_tombstone_cleanup_seconds_sum,prometheus_tsdb_vertical_compactions_total,prometheus_tsdb_wal_completed_pages_total,prometheus_tsdb_wal_corruptions_total,prometheus_tsdb_wal_fsync_duration_seconds,prometheus_tsdb_wal_fsync_duration_seconds_count,prometheus_tsdb_wal_fsync_duration_seconds_sum,prometheus_tsdb_wal_page_flushes_total,prometheus_tsdb_wal_segment_current,prometheus_tsdb_wal_truncate_duration_seconds_count,prometheus_tsdb_wal_truncate_duration_seconds_sum,prometheus_tsdb_wal_truncations_failed_total,prometheus_tsdb_wal_truncations_total,prometheus_tsdb_wal_writes_failed_total,prometheus_web_federation_errors_total,prometheus_web_federation_warnings_total,promhttp_metric_handler_requests_in_flight,promhttp_metric_handler_requests_total,rest_client_exec_plugin_certificate_rotation_age_bucket,rest_client_exec_plugin_certificate_rotation_age_count,rest_client_exec_plugin_certificate_rotation_age_sum,rest_client_exec_plugin_ttl_seconds,rest_client_request_duration_seconds_bucket,rest_client_request_duration_seconds_count,rest_client_request_duration_seconds_sum,rest_client_requests_total,scrape_duration_seconds,scrape_samples_post_metric_relabeling,scrape_samples_scraped,scrape_series_added,ssh_tunnel_open_count,ssh_tunnel_open_fail_count,storage_operation_duration_seconds_bucket,storage_operation_duration_seconds_count,storage_operation_duration_seconds_sum,storage_operation_errors_total,storage_operation_status_count,up,volume_manager_total_volumes,workqueue_adds_total,workqueue_depth,workqueue_longest_running_processor_seconds,workqueue_queue_duration_seconds_bucket,workqueue_queue_duration_seconds_count,workqueue_queue_duration_seconds_sum,workqueue_retries_total,workqueue_unfinished_work_seconds,workqueue_work_duration_seconds_bucket,workqueue_work_duration_seconds_count,workqueue_work_duration_seconds_sum"
				 },
				 "batchData": [{
				   "container": "conversationnotes",
				   "image": "us.gcr.io/cloud-bots/conversationnotes@sha256:3254ee07f695c6380c8f5d462f62cf3779d0f3346ffc012ba8b31aa7b1f2a818",
				   "instance": "marvinthemartian.cloudlab.com",
				   "pod": "conversationnotes-678479b48c-5qn9f",
				   "kubernetes_io_arch": "amd64",
				   "beta_kubernetes_io_os": "linux",
				   "kubernetes_io_hostname": "marvinthemartian.cloudlab.com",
				   "beta_kubernetes_io_arch": "amd64",
				   "kubernetes_io_os": "linux",
				   "container_file_descriptors": "0",
				   "__name__": "container_file_descriptors",
				   "name": "k8s_conversationnotes_conversationnotes-678479b48c-5qn9f_default_7de9a3cf-d707-4c37-b77f-3bcd7fbfa7af_4165",
				   "namespace": "default",
				   "id": "/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod7de9a3cf_d707_4c37_b77f_3bcd7fbfa7af.slice/docker-4f90312140b3e8253f1caa00be1b8e03ac000cccae66f1446a79d786b4175006.scope",
				   "job": "kubernetes-nodes-cadvisor"
				   
				   "container_file_descriptors": "17",
				 },...],
				 "listenerName": "Kube1",
				 "batchDate": 1596294060780
				}
			 */
		}
		
		private void addField (String name , int loop) {
			if ( loop > 0) {
				return;
			}
			try {
				JSONObject params 	= parent.getParams();
				String fields 		= params.getString("@fields");
				
				// reject duplicates
				if ( fields.contains(name + "," )) {
					return;
				}
				params.put("@fields", fields + "," + name); 
			}
			catch (Exception e) {
			}
		}
	}
	
	public PrometheusDataSource(final String name, final String description, final String url, long frequency, final IBatchEventListener listener) throws IOException, JSONException {
		super(DataSourceType.PROMETHEUS, name, description, null); //, new PollerEvents(name, url, frequency, listener));
		
		// trim last /
		this.url 		= url.endsWith( "/") ? url.substring(0, url.length() - 1) :  url;
		this.frequency	= frequency;
		super.events	= new PollerEvents(this /*name, url, frequency */, listener);
		
		initialize();
	}

	/**
	 * Construct from a JSON object.
	 * @param ds
	 * @throws JSONException 
	 * @throws IOException 
	 */
	public PrometheusDataSource ( JSONObject ds) throws JSONException, IOException {
		super(ds); 
		url 		= params.getString(KEY_PM_URL);
		frequency 	= params.optLong(KEY_PM_FREQ);
		
		events		= new PollerEvents(/*super.getName(), url , frequency*/ this, null);
		initialize();
	}

	public Set<String> getQueryFiels () {
		return Collections.unmodifiableSet(((PollerEvents) events).queryFields);
	}
	
	private void initialize () throws IOException, JSONException  {
		fetchFields();
		loadDefaultMetaData();
	}
	
	/**
	 * Get query metrics.
	 */
	private void fetchFields () throws IOException, JSONException {
		final String url1 	= url + "/api/v1/label/__name__/values?_=" + System.currentTimeMillis();
		WebClient wc 		= new WebClient(url1);
		final String out	= wc.doGet();
		
		
		if ( wc.getResponseCode() >= 300) {
			throw new IOException("Unable to fetch Prometheus meta-data from " + url1 + " HTTP Status code: " + wc.getResponseCode());
		}
		
		// {"status":"success","data":["F1","F2"....]}
		JSONObject json 	= new JSONObject(out);
		JSONArray data		= json.getJSONArray("data");
		final String rawCSV = data.toString().replaceAll("[\\\"\\[\\]]", "");

		log.debug("Fetch metric fields from url " + url1 + " Resp: " + wc.getResponseCode() + " # fields:" + rawCSV.split(",").length);
		
		// fmt has metric (display) fields
		fmt = new DataFormat(null, null, null, null, rawCSV, null);
		((PollerEvents) events).fmt = fmt;
	}

	private void loadDefaultMetaData () throws IOException, JSONException {
		Properties props 	= new Properties();
		InputStream is 		=  null;
		try {
			is = this.getClass().getResourceAsStream("/com/rts/datasource/ext/prometheus.properties");
			props.load(is);
			
			log.debug("loadDefaultMetaData Using default GROUPBY fields: " + props.getProperty("@fields"));
			
			// These are GROUP BY fields (not metrics)
			params.put("@fields", props.getProperty("@fields"));
		}
		finally {
			IOTools.closeStream(is);
		}
	}
	

	/**
	 * Add a (Dashboard, metric) tuple to the data source.
	 * @param dashb Dashboard id (unique title).
	 * @param metric Metric name. Note: It may contain a query METRIC{QUERY}
	 */
	public void addMetric ( final String dashb, final String metric ) {
		LOGD("Add dash: " + dashb + " metric: " + metric);
		
		// remove it first (in case it already exists) - minus query
		String name = metric.contains("{") ? metric.substring(0, metric.indexOf("{")) : metric;
		removeMetric(dashb, name) ; //metric);
		
		// add it.
		((PollerEvents) events).metrics.add(new DashMetricTuple(dashb, metric)); 
	}

	/**
	 * Remove a (Dashboard, metric) tuple to the data source.
	 * @param dashb Dashboard name.
	 * @param metric Metric name (does not contain a query).
	 */
	public void removeMetric ( final String dashb, final String metric ) {
		LOGD("Removing dash: " + dashb + " metric: " + metric);
		
		for ( DashMetricTuple dm : ((PollerEvents) events).metrics) {
			// Note: dm.metric may contain a query metric{....}
			if ( dm.dash.equals(dashb) && dm.metric.contains(metric)) {
				((PollerEvents) events).metrics.remove(dm);
				LOGD("Removed: " + dm);

			}
		}
	}

	@Override
	public void run() {
		LOGD("Running " + getName());
		
		synchronized ( this) {
			if ( status.getStatus() == Status.CONNECTING || status.getStatus() == Status.ON_LINE) {
				LOGW("Datasource " + name + " already started. Abort start.");
				return;
			}
			
			try {
				status.setStatus(Status.CONNECTING, "Coonecting to " + url);
				pollerStart();
				status.setStatus(Status.ON_LINE, "Ready " + url);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		try {
			pollerStop();
			status.setStatus(Status.OFF_LINE, "");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void shutdown() {
		stop();
	}

	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DataFormat getFormat() {
		return fmt;
	}

	@Override
	public long getTotalBatches() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getTotalRecords() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setEventListener(IBatchEventListener l) {
		// Set the batch receiver
		((PollerEvents) events).listener = l;
	}

	@Override
	public String toXML() throws IOException {
		throw new IOException("toXML() is deprecated.");	
	}

	@Override
	public DataSourceType getType() {
		return DataSourceType.PROMETHEUS;
	}

	/**
	 * Serialize to JSON.
	 * @return { "type": TYPE, "name": "NAME", "description": "DESC", "params": {"fsPath": "PATH"} }
	 */
	@Override
	public JSONObject toJSON() throws JSONException {
		JSONObject root = super.toJSON();

		params.put(KEY_PM_URL, url);
		params.put(KEY_PM_FREQ, frequency);
		
		root.put("params", params);
		
		return root;
	}

	@Override
	public JSONObject getParams() {
		return params;
	}

	/**
	 * Validate a metric
	 * @param query container_cpu_load_average_10s{namespace=\"westlake-dev\"}
	 * @throws IOException If invalid
	 */
	public void validate ( final String query ) throws IOException {
		final String uri = url + "/api/v1/query?query=" + query;
		LOGD("Validate " + uri);
		WebClient wc = new WebClient(uri);
		try {
			JSONObject json = new JSONObject(wc.doGet());
			checkStatus(getName(), json);
		} catch (Exception e) {
			throw new IOException(e.getMessage());
		}
	}

	private void checkStatus (String label, JSONObject json) throws IOException, JSONException {
		String status 		= json.getString("status"); // success, error
		
		// check the status
		if ( status.equalsIgnoreCase("error")) {
			throw new IOException(label + " (" + json.getString("errorType") + "): " + json.getString("error"));
		}
	}
	
	/**
	 * Get polling metric count.
	 * @return The number of metrics to be polled.
	 */
	public int getMetricCount () {
		return ((PollerEvents) events).metrics.size();
	}
}

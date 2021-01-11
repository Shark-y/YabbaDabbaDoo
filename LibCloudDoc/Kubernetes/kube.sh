###########################################################################################
# General purpose kubernetes install/setup script for CentOS7
# https://www.techrepublic.com/article/how-to-install-a-kubernetes-cluster-on-centos-7/
# https://github.com/kubernetes/kubernetes/issues/53333
# https://kubernetes.io/docs/setup/independent/create-cluster-kubeadm/
# define acct permissions 
# kubectl create clusterrolebinding default-admin --clusterrole cluster-admin --serviceaccount=default:default
###########################################################################################

# Install in the master (API server)
if [ $1 == "install" ] ; then
 echo "Selinux changes..."
 setenforce 0
 sed -i --follow-symlinks 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/sysconfig/selinux

 if [ $? -ne 0 ] ; then echo "Failed to disable Selinux" ; exit 1 ; fi

 # 1. disable swap
 swapoff -a
 if [ $? -ne 0 ] ; then echo "Failed to disable swap" ; exit 1 ; fi

 # 2. "*do* send the packets to iptables
 # (see config) modprobe br_netfilter
 # manually: echo '1' > /proc/sys/net/bridge/bridge-nf-call-iptables
 #sysctl net.bridge.bridge-nf-call-iptables=1
 
 #if [ $? -ne 0 ] ; then echo "Failed configure iptables" ; exit 1 ; fi
 
 # 3. Install docker
 echo "Installing docker..."
 yum install -y -q docker-io

 # 4. create k8s repo
 echo "Creating a k8s repo @ /etc/yum.repos.d/kuberetes.repo"
 cat <<EOF >> /etc/yum.repos.d/kuberetes.repo
[kubernetes]
name=Kubernetes
baseurl=https://packages.cloud.google.com/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=1
repo_gpgcheck=1
gpgkey=https://packages.cloud.google.com/yum/doc/yum-key.gpg
        https://packages.cloud.google.com/yum/doc/rpm-package-key.gpg
EOF

 # 5. install kubernetes
 echo "Installing k8s..."
 yum install -y -q kubelet kubeadm kubectl

 # 6. enable service
 echo "Enabling kubelet service..."
 systemctl enable kubelet.service

 # cgroup changes (deprecated)
 #echo "cgroup changes..."
 #sed -i 's/cgroup-driver=systemd/cgroup-driver=cgroupfs/g' /etc/systemd/system/kubelet.service.d/10-kubeadm.conf
 
 # 7. start service
 echo "Restarting systemd daemon"
 systemctl daemon-reload
 systemctl restart kubelet
fi

# get host IP adddress
addr=`hostname --ip-address`

# Initialize the container network (done in the API server only)
# https://coreos.com/flannel/docs/latest/
if [ $1 == "initflannel" ] ; then
 #Initialize the Kubernetes cluster
 echo "Configuring API server with IP $addr Flannel network"
 kubeadm init --apiserver-advertise-address=$addr --pod-network-cidr=10.244.0.0/16
 
 # for  connection to the server localhost:8080 was refusedhttps://github.com/kubernetes/kubernetes/issues/50295
 cp /etc/kubernetes/admin.conf $HOME/
 export KUBECONFIG=$HOME/admin.conf
 echo "export KUBECONFIG=$HOME/admin.conf" >> $HOME/.bashrc
 kubectl --namespace="kube-system" get all
 
fi

# Another popular network: https://www.projectcalico.org/
if [ $1 == "initcalico" ] ; then
 #Initialize the Kubernetes cluster
 echo "Configuring API server with IP $addr CALICO network"
 kubeadm init --apiserver-advertise-address=$addr --pod-network-cidr=192.168.0.0/16
 
 # Setup config: for  connection to the server localhost:8080 was refused 
 # https://github.com/kubernetes/kubernetes/issues/50295
 cp /etc/kubernetes/admin.conf $HOME/
 export KUBECONFIG=$HOME/admin.conf
 echo "export KUBECONFIG=$HOME/admin.conf" >> $HOME/.bashrc
 kubectl --namespace="kube-system" get all

fi

# Install network (pick one only flannel or calico) API server only
if [ $1 == "flannel" ] ; then
  kubectl apply -f https://raw.githubusercontent.com/coreos/flannel/master/Documentation/kube-flannel.yml
fi

if [ $1 == "calico" ] ; then
  kubectl apply -f https://docs.projectcalico.org/v3.1/getting-started/kubernetes/installation/hosted/rbac-kdd.yaml
fi

# For a slave node only (generate a join token)
# kubeadm token generate
# kubeadm token create <generated-token> --print-join-command --ttl=0
# https://stackoverflow.com/questions/40009831/cant-find-kubeadm-token-after-initializing-master
if [ $1 == "join" ] ; then
  master=$2
  TOKEN=$3
  DISCOVERY_TOKEN=$4
  echo "Joinig master $master token $TOKEN Discovery Token $DISCOVERY_TOKEN"
  kubeadm join $master:6443 --token $TOKEN --discovery-token-ca-cert-hash $DISCOVERY_TOKEN
fi

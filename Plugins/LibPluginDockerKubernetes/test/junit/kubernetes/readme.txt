QUICK RUN nginx: https://www.linode.com/docs/applications/containers/how-to-deploy-nginx-on-a-kubernetes-cluster/
-------------------------------------------------------------------------
1) Create deployment: 
	kubectl create deployment nginx --image=nginx
2) Make the NGINX container accessible via the internet:
	kubectl create service nodeport nginx --tcp=80:80
3) Look @ the ports
{

    port: 80,
    protocol: "TCP",
    nodePort: 30037,
    targetPort: 80,
    name: "80-80"

}
4) Access via worker  (kube1) IP:PORT http://192.168.42.121:30037/

Secret JSON (token)
--------------
{
    "apiVersion": "v1",
    "items": [
        {
            "apiVersion": "v1",
            "data": {
                "ca.crt": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUN5RENDQWJDZ0F3SUJBZ0lCQURBTkJna3Foa2lHOXcwQkFRc0ZBREFWTVJNd0VRWURWUVFERXdwcmRXSmwKY201bGRHVnpNQjRYRFRFNU1EUXdOVEV5TXprek5Wb1hEVEk1TURRd01qRXlNemt6TlZvd0ZURVRNQkVHQTFVRQpBeE1LYTNWaVpYSnVaWFJsY3pDQ0FTSXdEUVlKS29aSWh2Y05BUUVCQlFBRGdnRVBBRENDQVFvQ2dnRUJBT0JnCjU1YUlEWmYwYVNnb0N2MUdRaWFsODlZcEcvMGVSZC9oZE5TSW9nakJXd1FGZTNKY3M3TWp4KzZCUzQzcGdPbTAKenQxSFg5M2s5TFIyNFBOb3VEVE80YUZtbGplRGhiSFBmUjl6VVdEbXI4dk50UkUzT2ZrbHJ5QWEvaVpiamJYYwpYVFFFOFBRcXBrbUxneVhUMStzbE1lekFMeHNiMVA5Z0d1M2pKZ2dZNi8vd3dFd3hHSy9sMjQvU1Y0WkpLNzFpCmZ3eXkrb1lnbUw4c2VFbXBqdnczTkxjN2ZBTE1XSm94a2VrbmFpY3RNUnlTU3pQUGExbG5ESENKSnZ6YUx5WnQKVEFRWm1qNGFMRXpFcjhSRTNkblE1TVB1LzJ3UzlHeEhZSjBKK0RXNExIZ1pERERGTXZzL2xNYTA1bVUyOHRVYgpXU3NqYVM3MFFUMXE1MXNRU25VQ0F3RUFBYU1qTUNFd0RnWURWUjBQQVFIL0JBUURBZ0trTUE4R0ExVWRFd0VCCi93UUZNQU1CQWY4d0RRWUpLb1pJaHZjTkFRRUxCUUFEZ2dFQkFJdjh0MkRhYmlwSTdOVFVrNGdVZk5pQ1JwVGUKaGZuaU44N3Vtcnc1MFRldCtXNVFHNUYrM2s5d3RzTHFUamFhbkdRKy81cTdvUVRpNCtoTHdTUDJGK254UUNwbQo4Y2wvRkxTNi9QaUlYT1FwbXZvTGhwRmVOTHNwbCsvTjVLeXNuNExmbWlXZndsQjBtV1V4MlFIcnpTWWxpUks2Ck13cFlZRDhvQzBHSXVBWkxnZ3RuNGtWS3c2bXlVbk83TmY1Vk5pMGZLbktsMWppbm1STmZnZWdoaXZJcDdpV28KWjkwWTdsdkZDT0ZjZnRSdmFhM09STUhqWW93eDVzWjVGZXN0RDBmZGhKZnFUQVVLL0tpK0FxOG90TEp2SW15ago5cjNvMFlBVTN2dzMySXgrSjd2V0NWRFlLVG9yU0Q2TEpLZHFoT3ZrelZIV3NubHpoQjUxaUFZYWNXWT0KLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=",
                "namespace": "ZGVmYXVsdA==",
                "token": "ZXlKaGJHY2lPaUpTVXpJMU5pSXNJbXRwWkNJNklpSjkuZXlKcGMzTWlPaUpyZFdKbGNtNWxkR1Z6TDNObGNuWnBZMlZoWTJOdmRXNTBJaXdpYTNWaVpYSnVaWFJsY3k1cGJ5OXpaWEoyYVdObFlXTmpiM1Z1ZEM5dVlXMWxjM0JoWTJVaU9pSmtaV1poZFd4MElpd2lhM1ZpWlhKdVpYUmxjeTVwYnk5elpYSjJhV05sWVdOamIzVnVkQzl6WldOeVpYUXVibUZ0WlNJNkltUmxabUYxYkhRdGRHOXJaVzR0TkdNMmMzZ2lMQ0pyZFdKbGNtNWxkR1Z6TG1sdkwzTmxjblpwWTJWaFkyTnZkVzUwTDNObGNuWnBZMlV0WVdOamIzVnVkQzV1WVcxbElqb2laR1ZtWVhWc2RDSXNJbXQxWW1WeWJtVjBaWE11YVc4dmMyVnlkbWxqWldGalkyOTFiblF2YzJWeWRtbGpaUzFoWTJOdmRXNTBMblZwWkNJNkltWTJaVEptWm1OaUxUVTNPV1l0TVRGbE9TMWhOR1E1TFRBNE1EQXlOelU0WVROaU5TSXNJbk4xWWlJNkluTjVjM1JsYlRwelpYSjJhV05sWVdOamIzVnVkRHBrWldaaGRXeDBPbVJsWm1GMWJIUWlmUS5WQThHVjEzcFlVSlc0bml0NzYxQWZHTks3MXVLdXNlVGJ4Y2FEMVJUa0MtQktQd2NSNlZkcmdtY0RsWllvNGNRTlhXYzJqYVJETVk5bFZQRGNabmNSWThfbmZOTUNRel9jRmdVT3dGRHJZQWRIZ2M4ajh1SGIzQURWbW1aZ3VtVTJnV1N2bWx1MG1EYVR6M1dBRUhIYWs4OEstYm1XYzRwNEN2Q3AxbElxV1RJQzdQS19NSDRJcUFmcmpuVGVqbFdzLWttbGFVenpNRndBMWtfNDY2MjhZUUtFREo1YWxfaDBYTUZuWE1jaDVDMHEySndfem8wZVVpQU9pcVZuR1hxNkMybFdtRHJWc2pmVXdjdlI4Mk91aGlINVRmUVlLQjgyOEFYRGlUMmhheWZfMy1vRWlLQ0Q2WDdzTF9JYlVQdDloWE0xOGpzM2kxS0hoZExDdUhQQ2c="
            },
            "kind": "Secret",
            "metadata": {
                "annotations": {
                    "kubernetes.io/service-account.name": "default",
                    "kubernetes.io/service-account.uid": "f6e2ffcb-579f-11e9-a4d9-08002758a3b5"
                },
                "creationTimestamp": "2019-04-05T12:40:16Z",
                "name": "default-token-4c6sx",
                "namespace": "default",
                "resourceVersion": "317",
                "selfLink": "/api/v1/namespaces/default/secrets/default-token-4c6sx",
                "uid": "f6e877a0-579f-11e9-a4d9-08002758a3b5"
            },
            "type": "kubernetes.io/service-account-token"
        }
    ],
    "kind": "List",
    "metadata": {
        "resourceVersion": "",
        "selfLink": ""
    }
}

Kubernetes secrets
------------------
* Pull an Image from a Private Registry - https://kubernetes.io/docs/tasks/configure-pod-container/pull-image-private-registry/

* Add ImagePullSecrets to a service account - https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/#add-imagepullsecrets-to-a-service-account

	kubectl patch serviceaccount default -p '{"imagePullSecrets": [{"name": "regcred"}]}'

 Permission error? - see https://github.com/fabric8io/fabric8/issues/6840
 Need cluster-admin role based access to read stuff
  ------------------------------------------------------------------------
 
[WWWW]  ==== RESPONSE HTTP Status: 403
[GET] HTTP Response msg: Forbidden
[GET]  [HDR] null = [HTTP/1.1 403 Forbidden]
[GET]  [HDR] Date = [Thu, 04 Apr 2019 01:53:17 GMT]
[GET]  [HDR] Content-Length = [323]
[GET]  [HDR] Content-Type = [application/json]
[GET]  [HDR] X-Content-Type-Options = [nosniff]
[K8S-WORKLOAD] Get all pods NS response {
  "kind": "Status",
  "apiVersion": "v1",
  "metadata": {
    
  },
  "status": "Failure",
  "message": "pods is forbidden: User \"system:serviceaccount:default:default\" cannot list resource \"pods\" in API group \"\" at the cluster scope",
  "reason": "Forbidden",
  "details": {
    "kind": "pods"
  },
  "code": 403
}

========= FIX Create fabric8-rbac.yaml then 
kubectl apply -f fabric8-rbac.yaml  OR unbind with kubectl delete -f fabric8-rbac.yaml (see https://github.com/fabric8io/fabric8/issues/6840)
-----------------------------

# File: fabric8-rbac.yaml
# NOTE: The service account `default:default` already exists in k8s cluster.
# You can create a new account following like this:
#---
#apiVersion: v1
#kind: ServiceAccount
#metadata:
#  name: <new-account-name>
#  namespace: <namespace>

---
apiVersion: rbac.authorization.k8s.io/v1beta1
kind: ClusterRoleBinding
metadata:
  name: fabric8-rbac
subjects:
  - kind: ServiceAccount
    # Reference to upper's `metadata.name`
    name: default
    # Reference to upper's `metadata.namespace`
    namespace: default
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io 

# EDO : fabric8-rbac.yaml

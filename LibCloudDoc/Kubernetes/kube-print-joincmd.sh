JTOKEN=`kubeadm token generate`
kubeadm token create $JTOKEN --print-join-command --ttl=0
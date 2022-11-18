参考：
- https://github.com/fabric8io/kubernetes-client/blob/master/doc/CHEATSHEET.md
- https://github.com/fabric8io/kubernetes-client/tree/master/kubernetes-examples/src/main/java/io/fabric8/kubernetes/examples


准备demo镜像
```shell
docker pull httpd:2.4
docker tag httpd:2.4 registry.mufankong.top/bigdata/httpd:2.4
docker push registry.mufankong.top/bigdata/httpd:2.4
```
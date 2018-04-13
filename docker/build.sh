cp ../build/libs/*.jar .
cp ../xDSServer.properties .
docker build -t k8s_xds_server:v1.2 .
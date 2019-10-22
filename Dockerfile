FROM openjdk:8-jre-alpine

ARG toolVersion

RUN \
    apk add --no-cache bash python3 && \
    apk add --no-cache -t .required_apks python3-dev py-setuptools && \
    wget --no-check-certificate -O /tmp/flawfinder.tar.gz https://www.dwheeler.com/flawfinder/flawfinder-$toolVersion.tar.gz && \
    tar -zxf /tmp/flawfinder.tar.gz -C /tmp && \
    cd /tmp/flawfinder-$toolVersion && \
    python3 setup.py build && \
    python3 setup.py install --prefix=/usr --root=/ && \
    apk del .required_apks && \
    rm -rf /tmp/* && \
    rm -rf /var/cache/apk/*

ENTRYPOINT ["/usr/bin/flawfinder"]

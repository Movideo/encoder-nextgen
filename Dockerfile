FROM flurdy/oracle-java7
MAINTAINER DevOps <ops@movideo.com>

ADD build/distributions/encoder-nextgen-1.0.zip /opt/encoder-nextgen-1.0.zip

RUN apt-get -y install zip

RUN unzip -q encoder-nextgen-1.0.zip && \
    rm encoder-nextgen-1.0.zip && \
    mv encoder-nextgen-* encoder-nextgen

WORKDIR /opt/encoder-nextgen

ENTRYPOINT ["bin/encoder-nextgen"]

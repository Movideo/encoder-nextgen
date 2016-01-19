FROM flurdy/oracle-java7
MAINTAINER DevOps <ops@movideo.com>

# Set the locale
RUN locale-gen en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

WORKDIR /opt

ADD build/distributions/encoder-nextgen-1.0.zip /opt/encoder-nextgen-1.0.zip

RUN sudo apt-get update
RUN apt-get -y install unzip

RUN unzip -q /opt/encoder-nextgen-1.0.zip
RUN ls -la /opt/
RUN rm /opt/encoder-nextgen-1.0.zip
RUN mv /opt/encoder-nextgen-1.0 /opt/encoder-nextgen

WORKDIR /opt/encoder-nextgen

ENTRYPOINT ["bin/encoder-nextgen"]

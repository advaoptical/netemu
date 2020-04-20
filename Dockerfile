# Dockerfile for image: adva/netemu-dev

FROM ubuntu:focal

ENV DEBIAN_FRONTEND=noninteractive                                          \
    EDITOR=vim                                                              \
    JAVA_HOME=/usr/lib/jvm/default-java                                     \
    LANG=en_US.UTF-8                                                        \
    PAGER="less -rF"                                                        \
    TERM=xterm                                                              \
    TZ=UTC

RUN apt-get update                                                          \
&&  apt-get install --yes --no-install-recommends                           \
        locales                                                             \
                                                                            \
&&  locale-gen $LANG                                                        \
&&  update-locale                                                           \
                                                                            \
&&  apt-get install --yes --no-install-recommends                           \
        bash                                                                \
        curl                                                                \
        default-jdk                                                         \
        diffutils                                                           \
        gcc                                                                 \
        git                                                                 \
        less                                                                \
        patch                                                               \
        python3-dev                                                         \
        unzip                                                               \
        vim                                                                 \
        wget                                                                \
        zip                                                                 \
                                                                            \
&&  apt-get clean                                                           \
&&  rm -rf /var/lib/apt/lists/*                                             \
                                                                            \
&&  ln -s /usr/bin/python3 /usr/bin/python

RUN wget https://get.sdkman.io -O - | bash                                  \
&&  bash -c "source /root/.sdkman/bin/sdkman-init.sh                        \
    &&  sdk install gradle                                                  \
    &&  sdk install groovy                                                  \
    &&  sdk install maven                                                   \
    "

RUN wget https://bootstrap.pypa.io/get-pip.py -O - | python                 \
&&  pip install                                                             \
        ipython                                                             \
        jupyter                                                             \
        ncclient                                                            \
        pandas                                                              \
                                                                            \
&&  pip install jep

ADD . /opt/netemu

RUN cd /opt/netemu                                                          \
&&  bash -c "source /root/.sdkman/bin/sdkman-init.sh                        \
    &&  groovy mvn-yang-data-util.groovy install                            \
    &&  groovy mvn-yang-data-codec-xml.groovy install                       \
    &&  groovy mvn-netconf-netty-util.groovy install                        \
    &&  gradle install                                                      \
    "

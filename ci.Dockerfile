# Dockerfile for image: adtran/netemu-ci

FROM ubuntu:jammy

ARG SDK_JAVA_VERSION=17.0.10-tem
ARG SDK_GRADLE_VERSION=8.6
ARG SDK_GROOVY_VERSION=3.0.20
ARG SDK_MAVEN_VERSION=3.9.6

ENV DEBIAN_FRONTEND=noninteractive                                          \
    EDITOR=vim                                                              \
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
        ca-certificates                                                     \
        curl                                                                \
        git                                                                 \
        less                                                                \
        unzip                                                               \
        vim                                                                 \
        wget                                                                \
        zip                                                                 \
                                                                            \
&&  apt-get clean                                                           \
&&  rm -rf /var/lib/apt/lists/*

RUN wget https://get.sdkman.io -O - | bash                                  \
&&  bash -c "source /root/.sdkman/bin/sdkman-init.sh                        \
    &&  sdk install java $SDK_JAVA_VERSION                                  \
    &&  sdk install gradle $SDK_GRADLE_VERSION                              \
    &&  sdk install groovy $SDK_GROOVY_VERSION                              \
    &&  sdk install maven $SDK_MAVEN_VERSION                                \
    "

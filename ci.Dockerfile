# Dockerfile for image: gitlab.rd.advaoptical.com:4567/at/anden/netemu/ci

FROM ghcr.io/prefix-dev/pixi:trixie-slim

ARG PIXI_JAVA_VERSION=21.0.10
ARG PIXI_GRADLE_VERSION=8.11.1
ARG PIXI_GROOVY_VERSION=4.0.22
ARG PIXI_MAVEN_VERSION=3.9.16

ARG ODL_FORK_ACCOUNT=https://github.com/purplezimmermann
ARG ODL_FORK_BRANCH=adva/master

ARG ODL_YANGTOOLS_FORK_NAME=opendaylight-yangtools
ARG ODL_YANGTOOLS_FORK_URL="$ODL_FORK_ACCOUNT/$ODL_YANGTOOLS_FORK_NAME.git"

ARG ODL_MDSAL_FORK_NAME=opendaylight-mdsal
ARG ODL_MDSAL_FORK_URL="$ODL_FORK_ACCOUNT/$ODL_MDSAL_FORK_NAME.git"

ARG ODL_NETCONF_FORK_NAME=opendaylight-netconf
ARG ODL_NETCONF_FORK_URL="$ODL_FORK_ACCOUNT/$ODL_NETCONF_FORK_NAME.git"

ENV DEBIAN_FRONTEND=noninteractive                                          \
    TZ=UTC

RUN apt-get update                                                          \
&&  apt-get install --yes --no-install-recommends                           \
        locales                                                             \
        sed                                                                 \
                                                                            \
&&  apt-get clean                                                           \
&&  rm -rf /var/lib/apt/lists/*

ENV LANG=en_US.UTF-8

RUN sed --in-place --regexp-extended "s/^#\s*($LANG)/\1/" /etc/locale.gen   \
&&  locale-gen                                                              \
&&  update-locale LANG="$LANG"                                              \
                                                                            \
&&  apt-get update                                                          \
&&  apt-get install --yes --no-install-recommends                           \
        bash                                                                \
        bash-completion                                                     \
        ca-certificates                                                     \
                                                                            \
&&  apt-get clean                                                           \
&&  rm -rf /var/lib/apt/lists/*                                             \
                                                                            \
&&  pixi global install --environment base                                  \
        git                                                                 \
        gradle="$PIXI_GRADLE_VERSION"                                       \
        groovy="$PIXI_GROOVY_VERSION"                                       \
        less                                                                \
        maven="$PIXI_MAVEN_VERSION"                                         \
        openjdk="$PIXI_JAVA_VERSION"                                        \
        vim                                                                 \
                                                                            \
&& pixi clean cache --yes

ENV EDITOR=vim                                                              \
    PAGER="less -r"                                                         \
    TERM=xterm

ADD mvn-yangtools.groovy mvn-mdsal.groovy mvn-netconf.groovy /root/

WORKDIR /root

RUN git clone --single-branch --branch "$ODL_FORK_BRANCH" --            \
        "$ODL_YANGTOOLS_FORK_URL" "$ODL_YANGTOOLS_FORK_NAME"            \
                                                                        \
&&  groovy mvn-yangtools.groovy --batch-mode --projects=-karaf          \
        install --define skipTests                                      \
                --define maven.javadoc.skip=true                        \
                --define checkstyle.skip=true                           \
                --define spotbugs.skip=true                             \
                                                                        \
&&  rm --force --recursive "$ODL_YANGTOOLS_FORK_NAME"                   \
&&  rm mvn-yangtools.groovy                                             \
                                                                        \
&&  git clone --single-branch --branch "$ODL_FORK_BRANCH" --            \
        "$ODL_MDSAL_FORK_URL" "$ODL_MDSAL_FORK_NAME"                    \
                                                                        \
&&  groovy mvn-mdsal.groovy --batch-mode                                \
        install --define skipTests                                      \
                --define maven.javadoc.skip=true                        \
                --define checkstyle.skip=true                           \
                --define spotbugs.skip=true                             \
                                                                        \
&&  rm --force --recursive "$ODL_MDSAL_FORK_NAME"                       \
&&  rm mvn-mdsal.groovy                                                 \
                                                                        \
&&  git clone --single-branch --branch "$ODL_FORK_BRANCH" --            \
        "$ODL_NETCONF_FORK_URL" "$ODL_NETCONF_FORK_NAME"                \
                                                                        \
&&  groovy mvn-netconf.groovy --batch-mode                              \
        install --define skipTests                                      \
                --define maven.javadoc.skip=true                        \
                --define checkstyle.skip=true                           \
                --define spotbugs.skip=true                             \
                                                                        \
&&  rm --force --recursive "$ODL_NETCONF_FORK_NAME"                     \
&&  rm mvn-netconf.groovy

ENV CI=true

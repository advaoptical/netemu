# Dockerfile for image: adtran/netemu-ci

FROM ubuntu:jammy


ARG SDK_JAVA_VERSION=21.0.3-tem
ARG SDK_GRADLE_VERSION=8.7
ARG SDK_GROOVY_VERSION=3.0.21
ARG SDK_MAVEN_VERSION=3.9.6


ARG ODL_FORK_ACCOUNT=https://github.com/purplezimmermann
ARG ODL_FORK_BRANCH=adva/master

ARG ODL_YANGTOOLS_FORK_NAME=opendaylight-yangtools
ARG ODL_YANGTOOLS_FORK_URL="$ODL_FORK_ACCOUNT/$ODL_YANGTOOLS_FORK_NAME.git"

ARG ODL_MDSAL_FORK_NAME=opendaylight-mdsal
ARG ODL_MDSAL_FORK_URL="$ODL_FORK_ACCOUNT/$ODL_MDSAL_FORK_NAME.git"

ARG ODL_NETCONF_FORK_NAME=opendaylight-netconf
ARG ODL_NETCONF_FORK_URL="$ODL_FORK_ACCOUNT/$ODL_NETCONF_FORK_NAME.git"


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
                                                                            \
    &&  sdk install java "$SDK_JAVA_VERSION"                                \
    &&  sdk install gradle "$SDK_GRADLE_VERSION"                            \
    &&  sdk install groovy "$SDK_GROOVY_VERSION"                            \
    &&  sdk install maven "$SDK_MAVEN_VERSION"                              \
    "


ADD mvn-yangtools.groovy mvn-mdsal.groovy mvn-netconf.groovy /root/

RUN bash -c "cd /root && source .sdkman/bin/sdkman-init.sh                  \
                                                                            \
    &&  git clone --single-branch --branch "$ODL_FORK_BRANCH" --            \
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
    &&  rm mvn-netconf.groovy                                               \
    "

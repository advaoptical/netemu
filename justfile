build-docker: build-docker-gitlab-ci

build-docker-gitlab-ci:
    @echo "Building Docker image for GitLab CI ..."
    docker build . --file ci.Dockerfile --tag gitlab.rd.advaoptical.com:4567/at/anden/netemu/ci:latest

login-docker: login-docker-gitlab-ci

login-docker-gitlab-ci:
    @echo "Logging in to GitLab registry ..."
    docker login gitlab.rd.advaoptical.com:4567

push-docker: push-docker-gitlab-ci

push-docker-gitlab-ci:
    @echo "Pushing Docker image to GitLab registry ..."
    docker push gitlab.rd.advaoptical.com:4567/at/anden/netemu/ci:latest

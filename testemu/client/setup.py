# ADVA :: NETEMU >>> NETCONF/YANG-defined Enhanced Management Unit
#
# Copyright (C) 2020 ADVA Optical Networking
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from setuptools import find_packages, setup


setup(
    name='netemu-testemu-client',
    description="NETEMU Test client",

    author="Stefan Zimmermann",
    author_email="szimmermann@advaoptical.com",
    maintainer="ADVA Optical Networking",

    setup_requires="setuptools_scm >= 3.0.0",
    version_from_scm={'local_scheme': lambda version: ''},

    install_requires=open("requirements.txt").read(),
    packages=find_packages(include=["testemu_client", "testemu_client.*"]),
)

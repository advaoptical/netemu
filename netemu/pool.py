# ADVA :: NETEMU >>> NETCONF/YANG-driven Enhanced Management Unit
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

from abc import abstractmethod

from jep import PyJObject
import modeled

from .yangtools import NormalizedNode


class YangPool(modeled.object):
    """Pythonizer for Java class ``com.adva.netemu.YangPool``."""

    #: The Java ``YangPool`` instance.
    _java_pool = modeled.member.strict[PyJObject]()

    def __init__(self, java_pool):
        """Create from Java ``YangPool`` instance."""
        super().__init__(_java_pool=java_pool)

    @property
    def configuration_data(self):
        return ConfigurationData(self._java_pool)

    @property
    def operational_data(self):
        return OperationalData(self._java_pool)


class Data(modeled.object):

    #: The Java ``YangPool`` instance.
    _java_pool = modeled.member.strict[PyJObject]()

    def __init__(self, java_pool):
        super().__init__(_java_pool=java_pool)

    @abstractmethod
    def __call__(self):
        raise NotImplementedError(
            "'__call__' of {} is abstract".format(type(self)))


class ConfigurationData(Data):

    def __call__(self):
        """
        Read CONFIGURATION Data from Java ``YangPool`` instance.

        :return:
            Pythonized :class:`netemu.yangtools.NormalizedNode` object.
        """
        java_future_node = self._java_pool.readConfigurationData()
        java_optional_node = java_future_node.get()
        if java_optional_node.isEmpty():
            return None

        return NormalizedNode(java_optional_node.get())


class OperationalData(Data):

    def __call__(self):
        """
        Read OPERATIONAL Data from Java ``YangPool`` instance.

        :return:
            Pythonized :class:`netemu.yangtools.NormalizedNode` object.
        """
        java_future_node = self._java_pool.readOperationalData()
        java_optional_node = java_future_node.get()
        if java_optional_node.isEmpty():
            return None

        return NormalizedNode(java_optional_node.get())

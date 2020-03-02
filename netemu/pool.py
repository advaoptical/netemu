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

from abc import abstractmethod

import modeled
from jep import PyJObject

import netemu
from .yangtools import NormalizedNode


class Meta(modeled.object.meta):
    """
    Metaclass for :class:`netemu.YANGPool`.

    Defines inner classes for CONFIGURATION/OPERATIONAL Data access
    """

    class Data(modeled.object):
        """Abstract Data accessor for :class:`netemu.YANGPool`."""

        __package__ = netemu
        __qualname__ = 'YANGPool.Data'

        #: The Java ``com.adva.netemu.YangPool`` instance.
        _java_pool = modeled.member.strict[PyJObject]()

        def __init__(self, java_pool):
            """Create from Java ``com.adva.netemu.YangPool`` instance."""
            super().__init__(_java_pool=java_pool)

        @abstractmethod
        def __call__(self):
            """Read Data from Java ``com.adva.netemu.YangPool`` instance."""
            raise NotImplementedError(
                "'__call__' of {} is abstract".format(type(self)))

    class CONFIGURATION(Data):
        """CONFIGURATION Data accessor for :class:`netemu.YANGPool`."""

        __qualname__ = 'YANGPool.CONFIGURATION'

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

    class OPERATIONAL(Data):
        """OPERATIONAL Data accessor for :class:`netemu.YANGPool`."""

        __qualname__ = 'YANGPool.OPERATIONAL'

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


class YANGPool(modeled.object, metaclass=Meta):
    """Pythonizer for Java class ``com.adva.netemu.YangPool``."""

    __package__ = netemu

    #: The Java ``com.adva.netemu.YangPool`` instance.
    _java_pool = modeled.member.strict[PyJObject]()

    def __init__(self, java_pool):
        """Create from Java ``com.adva.netemu.YangPool`` instance."""
        super().__init__(_java_pool=java_pool)

    @property
    def configuration_data(self):
        """Get :class:`netemu.YANGPool.CONFIGURATION` Data accessor."""
        return type(self).CONFIGURATION(self._java_pool)

    @property
    def operational_data(self):
        """Get :class:`netemu.YANGPool.OPERATIONAL` Data accessor."""
        return type(self).OPERATIONAL(self._java_pool)

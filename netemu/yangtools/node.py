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

from itertools import chain

from moretools import decamelize
import modeled


def pythonize(yang_name):
    """
    Convert a YANG node name into a Pythonic identifier name.

    >>> pythonize("node-name")
    'node_name'

    >>> pythonize("nodeName")
    'node_name'
    """
    return decamelize(yang_name).replace('-', '_')


class NormalizedNode(modeled.object):
    """
    Pythonizer for Java class ``NormalizedNode<?, ?>``.

    From Java package ``org.opendaylight.yangtools.yang.data.api.schema``

    Representing a YANG Data node
    """

    #: The Java ``Normalized<?, ?>`` instance.
    _java_node = None

    def __new__(cls, java_node):
        return super().__new__({
            'org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.'
            'ImmutableContainerNodeBuilder$ImmutableContainerNode':
                ContainerNode,

            'org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.'
            'ImmutableMapNodeBuilder$ImmutableMapNode':
                MapNode,

            'org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.'
            'ImmutableMapEntryNodeBuilder$ImmutableMapEntryNode':
                MapEntryNode,

            'org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.'
            'ImmutableLeafNodeBuilder$ImmutableLeafNode':
                LeafNode,

        }.get(java_node.java_name))

    def __init__(self, java_node):
        self._java_node = java_node

    def __str__(self):
        return str(self._java_node)

    def __repr__(self):
        return "<{} {!r}>".format(
            type(self), str(self._java_node.getNodeType()))


class ContainerNode(NormalizedNode):
    """
    Pythonizer for Java class ``ImmutableContainerNode``.

    From Java outer class ``ImmutableContainerNodeBuilder`` from Java package
    ``org.opendaylight.yangtools.yang.data.impl.schema.builder.impl``

    Representing a YANG Data _container_
    """

    def __call__(self):
        """
        Get a ``list`` of all child nodes.

        As instances of :class:`NormalizedNode`
        """
        return list(map(NormalizedNode, self._java_node.getValue()))

    def __dir__(self):
        """
        Extend ``object.__dir__`` with Pythonized child node local names.

        To access child nodes as dynamic attributes via :meth:`.__getattr__`
        """
        return list(chain(super().__dir__(), (
            pythonize(java_key.getNodeType().getLocalName())
            for java_key in self._java_node.getChildren().keySet())))

    def __getattr__(self, name):
        """Get child node with local name matching Pythonized `name`."""
        for java_key in self._java_node.getChildren().keySet():
            if (java_key.java_name !=
                    'org.opendaylight.yangtools.yang.data.api.'
                    'YangInstanceIdentifier$NodeIdentifier'):
                continue

            if pythonize(java_key.getNodeType().getLocalName()) == name:
                return NormalizedNode(
                    self._java_node.getChild(java_key).get())

        raise AttributeError(
            "{!r} has no attribute or child node {!r}".format(self, name))


class MapNode(NormalizedNode):
    """
    Pythonizer for Java class ``ImmutableMapNode``.

    From Java outer class ``ImmutableMapNodeBuilder`` from Java package
    ``org.opendaylight.yangtools.yang.data.impl.schema.builder.impl``

    Representing a YANG Data _list_
    """

    def __call__(self):
        """
        Get a ``list`` of all (key tuple, child node) tuples.

        With child nodes as instances of :class:`NormalizedNode`
        """
        return [
            (
                tuple(java_child_node.getIdentifier().values()),
                NormalizedNode(java_child_node))
            for java_child_node in self._java_node.getValue()]

    def __getitem__(self, key):
        if not isinstance(key, tuple):
            key = (key, )
        for java_child_node in self._java_node.getValue():
            if tuple(java_child_node.getIdentifier().values()) == key:
                return NormalizedNode(java_child_node)

        raise KeyError(key)


class MapEntryNode(ContainerNode):
    """
    Pythonizer for Java class ``ImmutableMapEntryNode``.

    From Java outer class ``ImmutableMapEntryNodeBuilder`` from Java package
    ``org.opendaylight.yangtools.yang.data.impl.schema.builder.impl``

    Representing a YANG Data _list_ item, which is also a _container_
    """


class LeafNode(NormalizedNode):
    """
    Pythonizer for Java class ``ImmutableLeafNode<?>``.

    From Java outer class ``ImmutableLeafNodeBuilder<?>`` from Java package
    ``org.opendaylight.yangtools.yang.data.impl.schema.builder.impl``

    Representing a YANG Data _leaf_
    """

    def __call__(self):
        """Get the value of this _leaf_ node."""
        return self._java_node.getValue()

    def __repr__(self):
        return "<{} {!r}: {!r}>".format(
            type(self), str(self._java_node.getNodeType()),
            self._java_node.getValue())

from collections import Mapping

from . import yang_models


class Meta(type):

    class Interface(
            yang_models
            .com_adva_netemu_testemu_client_TestInterface_YangListModel):
        """
        Pythonizer for Java class ``TestInterface``.

        From Java package ``com.adva.netemu.testemu.client``
        """


class TestNetwork(
        yang_models.com_adva_netemu_testemu_client_TestNetwork_YangModel,
        metaclass=Meta):
    """
    Pythonizer for Java class ``TestNetwork``.

    From Java package ``com.adva.netemu.testemu.client``
    """

    @property
    def interfaces(self):

        class List(Mapping):

            @staticmethod
            def __len__():
                return len(self._java_object.getInterfaces())

            @staticmethod
            def __iter__():
                for intf in self._java_object.getInterfaces():
                    yield type(self).Interface(intf)

            @staticmethod
            def __getitem__(name):
                for intf in List.__iter__():
                    if intf.name() == name:
                        return intf

                raise KeyError(name)

        return List()

import re

from lxml import etree
from ncclient.manager import Manager as NCManager, connect as ncconnect
from ncclient.operations import RPCReply
from path import Path


NETCONF_CLIENT: NCManager = None

NETCONF_REPLY: RPCReply = None


def connect(argline: str):
    host, portstr = argline.strip().split(':')
    port = int(portstr)

    global NETCONF_CLIENT
    NETCONF_CLIENT = ncconnect(
        host=host, port=port, username='admin', password='CHGME.1a',
        hostkey_verify=False)


def get(argline: str):
    global NETCONF_REPLY
    NETCONF_REPLY = NETCONF_CLIENT.get()


def edit(argline: str, argblock: str = None):
    if argblock is None:
        target, config_children = argline.strip().split(maxsplit=1)
    else:
        target = argline.strip()
        config_children = argblock.strip()

    config: etree._Element = etree.Element('config')
    config.append(etree.fromstring(config_children))

    global NETCONF_REPLY
    NETCONF_REPLY = NETCONF_CLIENT.edit_config(target=target, config=config)


def reply(argline: str):
    return NETCONF_REPLY


def save(argline: str):
    path = Path(argline.strip())
    if path.ext.lower() != '.xml':
        path += '.xml'

    xml = re.sub(r"\n\s+\n", r"\n", etree.tounicode(NETCONF_REPLY.data))
    path.write_text(xml)

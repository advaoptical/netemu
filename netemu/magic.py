import re

from lxml.etree import tounicode
from ncclient.manager import Manager as NCManager, connect as ncconnect
from ncclient.operations import GetReply as NCGetReply
from path import Path


NETCONF_CLIENT: NCManager = None

NETCONF_REPLY: NCGetReply = None


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


def save(argline: str):
    path = Path(argline.strip())
    if path.ext.lower() != '.xml':
        path += '.xml'

    xml = re.sub(r"\n\s+\n", r"\n", tounicode(NETCONF_REPLY.data))
    path.write_text(xml)

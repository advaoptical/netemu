from setuptools import find_packages, setup


setup(
    name='netemu',
    description="NETCONF/YANG-driven Enhanced Management Unit",

    setup_requires="setuptools_scm >= 3.0",
    version_from_scm={'local_scheme': lambda version: ''},

    install_requires=open("requirements.txt").read(),
    packages=find_packages(include=["netemu", "netemu.*"]),
)

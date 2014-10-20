from fabric.api import task

from fabtools import require
from fabtools.require import deb


@task
def provision():
    install_jdk7()
    install_solr(port=9001)
    install_solr(port=9002, zk_host='localhost:9001')


def install_solr(port, war_location='2.6.4', where='', zk_host=None, jetty_location=''):
    if zk_host is not None:
        print('')


def install_jdk7():
    require.deb.package('openjdk-7-jre-headless', update=True)
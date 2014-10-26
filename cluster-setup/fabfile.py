# coding=utf-8
from fabric.api import task
from fabric.context_managers import cd
from fabric.operations import run

from fabtools.vagrant import vagrant
from fabtools import require, oracle_jdk

SOLR_4_6_0_DOWNLOAD_URL = 'http://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz'
SOLR_4_6_0_LOCATION = "~/solr-4.6.0"
SOLR_4_6_0_DD_LOCATION = "~/solr-4.6.0-dd"


@task
def provision():
    install_dependencies()
    download_solr_distro()
    bootstrap_cluster(SOLR_4_6_0_LOCATION, 8000)
    bootstrap_cluster(SOLR_4_6_0_DD_LOCATION, 8100)


@task
def restart_clusters():
    run('killall -9 java || exit 0')
    bootstrap_cluster(SOLR_4_6_0_LOCATION, 8000)
    bootstrap_cluster(SOLR_4_6_0_DD_LOCATION, 8100)


def install_dependencies():
    oracle_jdk.install_from_oracle_site()
    require.deb.package('dtach', update=True)


def download_solr_distro():
    with cd('~/'):
        run('wget -o solr-download.log –q {0} || exit 0'.format(SOLR_4_6_0_DOWNLOAD_URL))
        run('tar xf solr-4.6.0.tgz {0}'.format(SOLR_4_6_0_LOCATION))


@task
def mv_solr():
    run('rm -rf {}'.format(SOLR_4_6_0_DD_LOCATION))
    run('mkdir -p {}'.format(SOLR_4_6_0_DD_LOCATION))
    run('mv /vagrant/example {}'.format(SOLR_4_6_0_DD_LOCATION))


def bootstrap_cluster(solr_home, port_base):
    with cd(solr_home):
        run('rm -rf node-1')
        run('rm -rf node-2')
        run('cp -r example node-1')
        run('cp -r example node-2')

        with cd('node-1'):
            run('cp /vagrant/jetty/jetty.xml etc/jetty.xml')
            run_in_bg('java '
                      '-Djetty.port={} '
                      '-DzkRun '
                      '-DnumShards=2 '
                      '-Dbootstrap_confdir=./solr/collection1/conf '
                      '-Dcollection.configName=myconf '
                      '-jar start.jar '
                      '>& /dev/null < /dev/null'.format(port_base + 1))

        with cd('node-2'):
            run('cp /vagrant/jetty/jetty.xml etc/jetty.xml')
            run_in_bg('java '
                      '-Djetty.port={} '
                      '-DzkHost=localhost:{} '
                      '-jar start.jar '
                      '>& /dev/null < /dev/null'.format(port_base + 2, port_base + 1001))


def run_in_bg(cmd):
    run("dtach -n `mktemp -u /tmp/dtach.XXXX` {}".format(cmd))

# coding=utf-8
from fabric.api import task
from fabric.context_managers import cd
from fabric.operations import run

from fabtools import require
from fabtools.require import deb

LOCATION_SOLR_4_6_0 = 'http://archive.apache.org/dist/lucene/solr/4.6.0/solr-4.6.0.tgz'


@task
def provision():
    install_dependencies()
    setup_cloud()


def install_dependencies():
    require.deb.package('openjdk-7-jre-headless', update=True)
    require.deb.package('dtach', update=True)


def setup_cloud():
    with cd('~/'):
        # run('wget –q {0}'.format(LOCATION_SOLR_4_6_0))
        run('tar xf solr-4.6.0.tgz solr-4.6.0')

    with cd('~/solr-4.6.0'):
        run('cp -r example node-1')
        run('cp -r example node-2')

    with cd('~/solr-4.6.0/node-1'):
        run_in_bg('java '
                  '-Djetty.port=9001 '
                  '-DzkRun '
                  '-DnumShards=2 '
                  '-Dbootstrap_confdir=./solr/collection1/conf '
                  '-Dcollection.configName=myconf '
                  '-jar start.jar '
                  '>& /dev/null < /dev/null')

    with cd('~/solr-4.6.0/node-2'):
        run_in_bg(' java '
                  '-Djetty.port=9002 '
                  '-DzkHost=localhost:10001 '
                  '-jar start.jar '
                  '>& /dev/null < /dev/null')


def run_in_bg(cmd, sockname='dtach'):
    run("dtach -n `mktemp -u /tmp/{}.XXXX` {}".format(sockname, cmd))
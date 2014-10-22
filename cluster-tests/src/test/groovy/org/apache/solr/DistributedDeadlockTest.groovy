package org.apache.solr

import spock.lang.Specification

class DistributedDeadlockTest extends Specification {

  def "should fail" () {
    expect:
    1 == 0
  }
}

package org.apache.solr

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.CloudSolrServer
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Callable
import java.util.concurrent.Executors

class DistributedDeadlockTest extends Specification {

  private static final int REQUEST_COUNT = 200

  CloudSolrServer server = new CloudSolrServer("localhost:10001").with {
    it.setDefaultCollection("collection1")
    it
  }

  def executor = Executors.newFixedThreadPool(20)

  def cleanup() {
    executor.shutdown()
    server.shutdown()
  }

  def "should fail"() {
    given:
    def conditions = new PollingConditions(timeout: 5, initialDelay: 1, delay: 0.5)
    def responses = []

    when:
    (1..REQUEST_COUNT).each {
      responses << executor.submit({
        server.query(new SolrQuery("*:*"))
      } as Callable)
    }

    then:
    conditions.eventually {
      assert responses.size() == REQUEST_COUNT
      assert responses.every {
        it.get() != null
      }
    }
  }
}

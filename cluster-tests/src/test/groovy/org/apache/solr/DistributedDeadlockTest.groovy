package org.apache.solr

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.impl.LBHttpSolrServer
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrInputDocument
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class DistributedDeadlockTest extends Specification {

  private static final int REQUEST_COUNT = 20

  @Shared
  def connections = connectToClusters()

  def executor = Executors.newFixedThreadPool(20)

  def cleanup() {
    executor.shutdown()
    connections.each {
      it.deleteByQuery("*:*")
      it.commit()
      it.shutdown()
    }
  }

  def setup() {
    connections.each {
      it.add(
          new SolrInputDocument().with {
            it.addField("id", "001")
            it.addField("name", "awesome document")
            it
          }
      )
      it.commit()
    }
  }

  def "should not detect any deadlocks"() {
    given:
    def conditions = new PollingConditions(timeout: 10, initialDelay: 1, delay: 0.5)
    List<Future<QueryResponse>> responses = []

    when:
    (1..REQUEST_COUNT).each {
      responses << executor.submit({
        conection.query(new SolrQuery("*:*"))
      } as Callable)
    }

    then:
    conditions.eventually {
      print("requests send: ${responses.size()}")
      assert responses.size() == REQUEST_COUNT
      assert responses.every {
        it.get(5, TimeUnit.SECONDS).getResults().size() == 1
      }
    }

    where:
    conection << connections
  }

  private List<SolrServer> connectToClusters() {
    [
        // vanilla Apache Solr 4.6.0
        new LBHttpSolrServer("http://localhost:9001/solr/collection1", "http://localhost:9002/solr/collection1")
        // Modified Apache Solr 4.6.0
    ]
  }
}

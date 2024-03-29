package org.apache.solr

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrServer
import org.apache.solr.client.solrj.impl.LBHttpSolrServer
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrInputDocument
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class DistributedDeadlockTest extends Specification {

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

  @Unroll("should not detect any deadlocks for cluster #conection")
  def "should not detect any deadlocks"() {
    given:
    int requestCount = 50
    List<Future<QueryResponse>> futures = []

    when:
    (1..requestCount).each {
      futures << executor.submit({
        conection.query(new SolrQuery("*:*"))
      } as Callable)
    }
    def responses = futures.collect {
      it.get(10, TimeUnit.SECONDS)
    }

    then:
    assert responses.size() == requestCount
    assert responses.every {
      it.getResults().size() == 1
    }

    where:
    conection << connections
  }

  private List<SolrServer> connectToClusters() {
    [
        // Modified Apache Solr 4.6.0
        new LBHttpSolrServer("http://localhost:8101/solr/collection1", "http://localhost:8102/solr/collection1")
        // vanilla Apache Solr 4.6.0
//        new LBHttpSolrServer("http://localhost:8001/solr/collection1", "http://localhost:8002/solr/collection1")
    ]
  }
}

solr-at
=======

### Automated Apache Solr cluster provisioning + distributed deadlock acceptance tests

#### How to start
VM setup uses [Vagrant](http://www.vagrantup.com/) and [fabric](http://www.fabfile.org/) provisioning. 
Assuming you have [pip](https://pip.pypa.io/en/latest/) installed, all python dependencies might be installed using

`pip install -r requirements.txt`

There is a vagrant plugin needed to run the box. Having Vagrant and Virtualbox installed, you may type

`vagrant plugin install vagrant-fabric`

Having vagrant, Vritualbox and all python dependencies installed, you can deal with the VM 
using common Vagrant commands, e.g.
`vagrant up`

#### Intended interaction

1. `cd cluster-setup` and `vagrant up` => provisions a VM
2. go to Solr sources directory and invoke `ant clean compile dist example` => builds Solr distribution
3. `cp -r example ~/your-location-of/solr-at/cluster-setup` => moves fresh Solr distribution into the host/guest shared directory
4. `fab vagrant mv_solr` => moves Solr example directory from shared directory into guest-only directory
5. `fab vagrant restart_clusters` => kills all Solr processes and starts 2 clusters (with same configuration): 
vanilla (ports 8001, 8002) and branch build (ports 8101, 8102)
6. `cd cluster-test` and `./gradlew clean test` => runs acceptance test against Solr clusters
7. Check the results and repeat from 2. if necessary.
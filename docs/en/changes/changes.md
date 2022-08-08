## 9.2.0

#### Project

* Upgrade Armeria to 1.16.0, Kubernetes Java client to 15.0.1.

#### OAP Server

* Add more entities for Zipkin to improve performance.
* ElasticSearch: scroll id should be updated when scrolling as it may change.
* Mesh: fix only last rule works when multiple rules are defined in metadata-service-mapping.yaml.
* Support sending alarm messages to PagerDuty.
* Support Zipkin kafka collector.
* Add `VIRTUAL` detect type to Process for Network Profiling.
* Add component ID(128) for Java Hutool plugin.
* Add Zipkin query exception handler, response error message for illegal arguments.
* Fix a NullPointerException in the endpoint analysis, which would cause missing MQ-related `LocalSpan` in the trace.
* Add `forEach`, `processRelation` function to MAL expression.
* Add `expPrefix`, `initExp` in MAL config.
* Add component ID(7015) for Python Bottle plugin.
* Remove legacy OAL `percentile` functions, `p99`, `p95`, `p90`, `p75`, `p50` func(s).
* Revert [#8066](https://github.com/apache/skywalking/pull/8066). Keep all metrics persistent even it is default value.
* Skip loading UI templates if folder is empty or doesn't exist.
* Optimize ElasticSearch query performance by using `_mGet` and physical index name rather than alias in these scenarios,  (a) Metrics aggregation (b) Zipkin query (c) Metrics query (d) Log query
* Support the `NETWORK` type of eBPF Profiling task.
* Support `sumHistogram` in `MAL`.
* [Breaking Change] Make the eBPF Profiling task support to the service instance level, index/table `ebpf_profiling_task` is required to be re-created when bump up from previous releases.
* Fix race condition in Banyandb storage
* Support `SUM_PER_MIN` downsampling in `MAL`.
* Support `sumHistogramPercentile` in `MAL`.
* Add `VIRTUAL_CACHE` to Layer, to fix conjectured Redis server, which icon can't show on the topology.
* [Breaking Change] Elasticsearch storage merge all metrics/meter and records(without super datasets) indices into one physical index template `metrics-all` and `records-all` on the default setting.
  Provide system environment variable(`SW_STORAGE_ES_LOGIC_SHARDING`) to shard metrics/records indices into multi-physical indices as the previous versions(one index template per metric/meter aggregation function).
  In the current one index mode, users still could choose to adjust ElasticSearch's shard number(`SW_STORAGE_ES_INDEX_SHARDS_NUMBER`) to scale out.
* [Breaking Change] Many columns of metrics and records model names are changed, The H2/Mysql/Tidb/Postgres storage users are required to remove all metrics-related and records-related tables for OAP to re-create or use a new database instance.
* Fix Zipkin trace query the max size of spans.
* Add `tls` and `https` component IDs for Network Profiling.

#### UI

* Fix query conditions for the browser logs.
* Implement a url parameter to activate tab index.
* Fix clear interval fail when switch autoRefresh to off.
* Optimize log tables.
* Fix log detail pop-up page doesn't work.
* Optimize table widget to hide the whole metric column when no metric is set.
* Implement the Event widget. Remove `event` menu.
* Fix span detail text overlap.
* Add Python Bottle Plugin Logo.
* Implement an association between widgets(line, bar, area graphs) with time.
* Fix tag dropdown style.
* Hide the copy button when db.statement is empty.
* Fix legend metrics for topology.
* Dashboard: Add metrics association.
* Dashboard: Fix `FaaS-Root` document link and topology service relation dashboard link.
* Dashboard: Fix `Mesh-Instance` metric `Throughput`.
* Dashboard: Fix `Mesh-Service-Relation` metric `Throughput` and `Proxy Sidecar Internal Latency in Nanoseconds (Client Response)`.
* Dashboard: Fix `Mesh-Instance-Relation` metric `Throughput`.
* Enhance associations for the Event widget.
* Add event widgets in dashboard where applicable.
* Fix dashboard list search box not work.
* Fix short time range.
* Fix event widget incompatibility in Safari.
* Refactor the tags component to support searching for tag keys and values.
* Implement the log widget and the trace widget associate with each other, remove log tables on the trace widget.
* Add log widget to general service root.
* Associate the event widget with the trace and log widget.
* Add the MYSQL layer and update layer routers.
* Fix query order for trace list.
* Add a calculation to convert seconds to days.

#### Documentation

* Fix invalid links in release docs.
* Clean up doc about event metrics.
* Add a table for metric calculations in the ui doc.

All issues and pull requests are [here](https://github.com/apache/skywalking/milestone/136?closed=1)

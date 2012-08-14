(ns clojurewerkz.cassaforte.thrift.core-test
  (:refer-clojure :exclude [get])
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.conversion :as conv]
            [clojurewerkz.cassaforte.thrift.keyspace :as k]
            [clojurewerkz.cassaforte.thrift.column-or-super-column :as cosc]
            )
  (:use clojurewerkz.cassaforte.thrift.core
        clojurewerkz.cassaforte.test.helper
        clojurewerkz.cassaforte.conversion
        clojure.test))

(def *consistency-level* (conv/to-consistency-level :one))
(cc/connect! "127.0.0.1" "CassaforteTest1")

(deftest t-batch-mutate
  (with-thrift-exception-handling
    (k/drop-keyspace "keyspace_name"))

  (k/add-keyspace
   (build-keyspace-definition "keyspace_name"
                              "org.apache.cassandra.locator.SimpleStrategy"
                              [(build-cfd "keyspace_name" "ColumnFamily2" [(build-cd "first" "UTF8Type")
                                                                           (build-cd "second" "UTF8Type")
                                                                           (build-cd "third" "UTF8Type")])]
                              :strategy-opts {"replication_factor" "1"}))

  (k/set-keyspace "keyspace_name")

  (batch-mutate
   {"key1" {"ColumnFamily2" {:first "a" :second "b"} }
    "key2" {"ColumnFamily2" {:first "c" :second "d"} }}
   *consistency-level*)

  (is (= {:first "a" :second "b"} (to-plain-hash (get-slice "ColumnFamily2" "key1" *consistency-level*))))
  (is (= {:first "c" :second "d"} (to-plain-hash (get-slice "ColumnFamily2" "key2" *consistency-level*)))))

(deftest t-batch-mutate-supercolumn
  (with-thrift-exception-handling
    (k/drop-keyspace "keyspace_name"))

  (k/add-keyspace
   (build-keyspace-definition "keyspace_name"
                              "org.apache.cassandra.locator.SimpleStrategy"
                              [(build-cfd "keyspace_name" "ColumnFamily1" [] :column-type "Super")]
                              :strategy-opts {"replication_factor" "1"}))
  (k/set-keyspace "keyspace_name")

  (batch-mutate
   {"key1" {"ColumnFamily1" {:name1 {:first "a" :second "b"} :name2 {:first "c" :second "d"}} }
    "key2" {"ColumnFamily1" {:name1 {:first "a" :second "b"} :name2 {:first "c" :second "d"}} }}
   *consistency-level*
   :type :super)

  (is (= {:name1 {:first "a" :second "b"} :name2 {:first "c" :second "d"}}
         (to-plain-hash (get-slice "ColumnFamily1" "key1" *consistency-level*)))))
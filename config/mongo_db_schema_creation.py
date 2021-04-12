## ============LICENSE_START=======================================================
## mongo_db_schema_creation
## ================================================================================
## Copyright (C) 2020 Nokia. All rights reserved.
## ================================================================================
## Licensed under the Apache License, Version 2.0 (the "License");
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##      http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
## ============LICENSE_END=========================================================

from pymongo import MongoClient
import argparse
import os

parser = argparse.ArgumentParser(description='Script creates configuration in pnf simulator mongodb if it doesn\'t exists.')
args = parser.parse_args()

MONGO_DB_PASSWORD = "zXcVbN123!"

def create_db_with_user_permissions(client):
    client.pnf_simulator.add_user( 'pnf_simulator_user',  MONGO_DB_PASSWORD, roles= [{'role':'readWrite', 'db':'pnf_simulator'},{'role':'dbAdmin', 'db':'pnf_simulator'}])

def init_mongo_db(client, col_list):
    if 'simulatorConfig' not in col_list:
        client.pnf_simulator.simulatorConfig.insert_one({"vesServerUrl": "https://dcae-ves-collector.onap:8443/eventListener/v7"})
    if 'template' not in col_list:
        client.pnf_simulator.create_collection('template')
    if 'flatTemplatesView' not in col_list:
        client.pnf_simulator.create_collection("flatTemplatesView", viewOn="template",pipeline=[{"$project":{"keyValues":{"$objectToArray": "$$ROOT.flatContent"}}}])


if __name__ == "__main__":
    client = MongoClient(host=os.getenv('MONGO_HOSTNAME', 'mongo'),
                     port=27017,
                     username='root',
                     password=MONGO_DB_PASSWORD,
                     authSource="admin")
    col_list=client.pnf_simulator.list_collection_names()
    create_db_with_user_permissions(client)
    init_mongo_db(client, col_list)
    print("Following colections are present in simualtor db: " ,  client.pnf_simulator.list_collection_names())

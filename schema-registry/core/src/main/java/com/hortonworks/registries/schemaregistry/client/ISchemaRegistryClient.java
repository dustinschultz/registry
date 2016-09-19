/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hortonworks.registries.schemaregistry.client;

import com.hortonworks.registries.schemaregistry.IncompatibleSchemaException;
import com.hortonworks.registries.schemaregistry.InvalidSchemaException;
import com.hortonworks.registries.schemaregistry.SchemaFieldQuery;
import com.hortonworks.registries.schemaregistry.SchemaInfo;
import com.hortonworks.registries.schemaregistry.SchemaKey;
import com.hortonworks.registries.schemaregistry.SchemaNotFoundException;
import com.hortonworks.registries.schemaregistry.SchemaVersion;
import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.SchemaVersionKey;
import com.hortonworks.registries.schemaregistry.SerDesInfo;
import com.hortonworks.registries.schemaregistry.serde.SerDesException;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;

/**
 * This interface defines different methods to interact with remote schema registry.
 * <p>
 * Below code describes how to register new schemas, add new version of a schema and fetch different versions of a schema.
 * <pre>
 *
 * // registering new schema-metadata
 * SchemaInfo schemaInfo = createSchemaInfo(TEST_NAME_RULE.getMethodName());
 * SchemaKey schemaKey = schemaInfo.getSchemaKey();
 *
 * // registering a new schema
 * Integer v1 = schemaRegistryClient.addSchemaVersion(schemaInfo, new SchemaVersion(schema1, "Initial version of the schema"));
 *
 * // adding a new version of the schema
 * SchemaVersion schemaInfo2 = new SchemaVersion(schema2, "second version");
 * Integer v2 = schemaRegistryClient.addSchemaVersion(schemaKey, schemaInfo2);
 *
 * // get the specific version of the schema
 * SchemaVersionInfo schemaVersionInfo = schemaRegistryClient.getSchemaVersionInfo(new SchemaVersionKey(schemaKey, v2));
 *
 * // get the latest version of the schema
 * SchemaVersionInfo latest = schemaRegistryClient.getLatestSchemaVersionInfo(schemaKey);
 *
 * // get all versions of a specific schema
 * Collection<SchemaVersionInfo> allVersions = schemaRegistryClient.getAllVersions(schemaKey);
 *
 * // get schema versions which have schema field swith name 'txid'
 * Collection<SchemaVersionKey> txidSchemaVersionKeys = schemaRegistryClient.findSchemasByFields(new SchemaFieldQuery.Builder().name("txid").build());
 *
 *
 * </pre>
 * <p>
 * Below code describes how to register serializer and deserializers, map them with a schema etc.
 * <pre>
 * // upload a jar containing serializer and deserializer classes.
 * InputStream inputStream = new FileInputStream("/schema-custom-ser-des.jar");
 * String fileId = schemaRegistryClient.uploadFile(inputStream);
 *
 * // add serializer with the respective uploaded jar file id.
 * SerDesInfo serializerInfo = createSerDesInfo(fileId);
 * Long serializerId = schemaRegistryClient.addSerializer(serializerInfo);
 *
 * schemaKey = new SchemaKey(type(), "kafka", "com.hwx.iot.device.schema");
 *
 * // map this serializer with a registered schema
 * schemaRegistryClient.mapSchemaWithSerDes(schemaKey, serializerId);
 *
 * // get registered serializers
 * Collection<SerDesInfo> serializers = schemaRegistryClient.getSerializers(schemaKey);
 * SerDesInfo registeredSerializerInfo = serializers.iterator().next();
 *
 * //get serializer and serialize the given payload
 * try(AvroSnapshotSerializer snapshotSerializer = schemaRegistryClient.createInstance(registeredSerializerInfo);) {
 * Map<String, Object> config = Collections.emptyMap();
 * snapshotSerializer.init(config);
 *
 * byte[] serializedData = snapshotSerializer.serialize(input, schemaInfo);
 *
 * </pre>
 */
public interface ISchemaRegistryClient extends AutoCloseable {

    /**
     * Registers information about a schema.
     *
     * @param schemaInfo information about schema.
     * @return true if the given {@code schemaInfo} is successfully registered.
     */
    boolean registerSchemaInfo(SchemaInfo schemaInfo) throws InvalidSchemaException;

    /**
     * Returns version of the schema added to the given schemaInfo.
     * <pre>
     * It tries to fetch an existing schema or register the given schema with the below conditions
     *  - Checks whether there exists a schema with the given schemaText, and schemaInfo#getSchemaKey()
     *      - returns respective schemaVersionKey if it exists.
     *      - Creates a schema for the given name and returns respective schemaVersionKey if it does not exist.
     * </pre>
     *
     * @param schemaInfo    information about the schema
     * @param schemaVersion new version of the schema to be registered
     * @return version of the schema added.
     * @throws InvalidSchemaException      if the given versionedSchema is not valid
     * @throws IncompatibleSchemaException if the given versionedSchema is incompatible according to the compatibility set.
     */
    Integer addSchemaVersion(SchemaInfo schemaInfo, SchemaVersion schemaVersion) throws InvalidSchemaException, IncompatibleSchemaException;

    /**
     * Adds the given {@code schemaVersion} and returns the corresponding version number.
     *
     * @param schemaKey     key identifying a schema
     * @param schemaVersion new version of the schema to be added
     * @return version number of the schema added
     * @throws InvalidSchemaException      if the given versionedSchema is not valid
     * @throws IncompatibleSchemaException if the given versionedSchema is incompatible according to the compatibility set.
     */
    Integer addSchemaVersion(SchemaKey schemaKey, SchemaVersion schemaVersion) throws InvalidSchemaException, IncompatibleSchemaException;

    /**
     * @return schema versions matching the fields specified in the query
     */
    Collection<SchemaVersionKey> findSchemasByFields(SchemaFieldQuery schemaFieldQuery);

    /**
     * @param schemaVersionKey key identifying a schema and a version
     * @return {@link SchemaVersionInfo} for the given {@link SchemaVersionKey}
     */
    SchemaVersionInfo getSchemaVersionInfo(SchemaVersionKey schemaVersionKey) throws SchemaNotFoundException;


    /**
     * @param schemaKey key identifying schema
     * @return latest version of the schema for the given {@param schemaMetadataKey}
     */
    SchemaVersionInfo getLatestSchemaVersionInfo(SchemaKey schemaKey) throws SchemaNotFoundException;

    /**
     * @param schemaKey key identifying schema
     * @return all versions of the schemas for given {@param schemaMetadataKey}
     */
    Collection<SchemaVersionInfo> getAllVersions(SchemaKey schemaKey) throws SchemaNotFoundException;


    /**
     * @param schemaKey    key identifying a schema
     * @param toSchemaText text representing the schema to be checked for compatibility
     * @return true if the given {@code toSchemaText} is compatible with the latest version of the schema with id as {@code schemaMetadataKey}.
     */
    boolean isCompatibleWithAllVersions(SchemaKey schemaKey, String toSchemaText) throws SchemaNotFoundException;

    /**
     * TODO: needs better description. What bytes are being uploaded?
     *
     * @param inputStream
     * @return unique id for the uploaded bytes read from input stream to file storage.
     */
    String uploadFile(InputStream inputStream) throws SerDesException;

    /**
     * Downloads the content of file stored with the given {@code fileId}.
     * TODO need description on what these files are
     *
     * @param fileId
     * @return
     */
    InputStream downloadFile(String fileId) throws FileNotFoundException;

    /**
     * Returns unique id for the added Serializer for the given {@code schemaSerializerInfo}
     *
     * @param serializerInfo
     * @return
     */
    Long addSerializer(SerDesInfo serializerInfo);

    /**
     * Returns unique id for the added Serializer for the given {@code schemaSerializerInfo}
     *
     * @param deserializerInfo
     * @return
     */
    Long addDeserializer(SerDesInfo deserializerInfo);

    /**
     * Maps Serializer/Deserializer of the given {@code serDesId} to Schema with {@code schemaMetadataKey}
     *
     * @param schemaKey
     * @param serDesId
     */
    void mapSchemaWithSerDes(SchemaKey schemaKey, Long serDesId);

    /**
     * Returns Collection of Serializers registered for the schema with {@code schemaMetadataKey}
     *
     * @param schemaKey
     * @return
     */
    Collection<SerDesInfo> getSerializers(SchemaKey schemaKey);

    /**
     * Returns Collection of Deserializers registered for the schema with {@code schemaMetadataKey}
     *
     * @param schemaKey
     * @return
     */
    Collection<SerDesInfo> getDeserializers(SchemaKey schemaKey);

    /**
     * Returns a new instance of the respective Serializer class for the given {@code serializerInfo}
     *
     * @param <T>            type of the instance to be created
     * @param serializerInfo
     * @throws SerDesException throws an Exception if serializer or deserializer class is not an instance of {@code T}
     */
    <T> T createSerializerInstance(SerDesInfo serializerInfo);

    /**
     * Returns a new instance of the respective Deserializer class for the given {@code deserializerInfo}
     *
     * @param <T>              type of the instance to be created
     * @param deserializerInfo
     * @throws SerDesException throws an Exception if serializer or deserializer class is not an instance of {@code T}
     */
    <T> T createDeserializerInstance(SerDesInfo deserializerInfo);

}
/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.apimgt.governance.impl.dao.impl;

import org.wso2.carbon.apimgt.governance.api.error.APIMGovernanceException;
import org.wso2.carbon.apimgt.governance.api.model.ExternalService;
import org.wso2.carbon.apimgt.governance.api.model.ExternalServiceHeader;
import org.wso2.carbon.apimgt.governance.api.model.ExternalServiceList;
import org.wso2.carbon.apimgt.governance.api.model.HeaderCategory;
import org.wso2.carbon.apimgt.governance.impl.dao.ExternalServiceMgtDAO;
import org.wso2.carbon.apimgt.governance.impl.util.APIMGovernanceDBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of External Service Management DAO.
 */
public class ExternalServiceMgtDAOImpl implements ExternalServiceMgtDAO {

    private static final String ADD_EXTERNAL_SERVICE_SQL = "INSERT INTO EXTERNAL_SERVICES (ID, NAME, URL, PROMPT, " +
            "TIMEOUT_MS, RETRY_COUNT, CREATED_AT) VALUES (?, ?, ?, ?, ?, ?, ?)";
        private static final String ADD_EXTERNAL_SERVICE_HEADER_SQL = "INSERT INTO EXTERNAL_SERVICE_HEADERS " +
            "(SERVICE_ID, HEADER_KEY, HEADER_VALUE, CATEGORY) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_EXTERNAL_SERVICE_SQL = "UPDATE EXTERNAL_SERVICES SET NAME = ?, URL = ?, " +
            "PROMPT = ?, TIMEOUT_MS = ?, RETRY_COUNT = ? WHERE ID = ?";
    private static final String DELETE_HEADERS_BY_SERVICE_ID_SQL =
 "DELETE FROM EXTERNAL_SERVICE_HEADERS WHERE SERVICE_ID = ?";
    private static final String DELETE_EXTERNAL_SERVICE_SQL = "DELETE FROM EXTERNAL_SERVICES WHERE ID = ?";
    private static final String GET_EXTERNAL_SERVICE_BY_ID_SQL = "SELECT ID, NAME, URL, PROMPT, TIMEOUT_MS, " +
            "RETRY_COUNT, CREATED_AT FROM EXTERNAL_SERVICES WHERE ID = ?";
    private static final String GET_HEADERS_BY_SERVICE_ID_SQL = "SELECT ID, SERVICE_ID, HEADER_KEY, HEADER_VALUE, " +
            "CATEGORY FROM EXTERNAL_SERVICE_HEADERS WHERE SERVICE_ID = ?";
    private static final String GET_ALL_EXTERNAL_SERVICES_SQL = "SELECT ID, NAME, URL, PROMPT, TIMEOUT_MS, " +
            "RETRY_COUNT, CREATED_AT FROM EXTERNAL_SERVICES";

    @Override
    public ExternalService addExternalService(ExternalService externalService) throws APIMGovernanceException {
        try (Connection connection = APIMGovernanceDBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (externalService.getId() == null) {
                    externalService.setId(UUID.randomUUID().toString());
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(ADD_EXTERNAL_SERVICE_SQL)) {
                    preparedStatement.setString(1, externalService.getId());
                    preparedStatement.setString(2, externalService.getName());
                    preparedStatement.setString(3, externalService.getUrl());
                    preparedStatement.setString(4, externalService.getPrompt());
                    if (externalService.getTimeoutMs() != null) {
                        preparedStatement.setInt(5, externalService.getTimeoutMs());
                    } else {
                        preparedStatement.setNull(5, java.sql.Types.INTEGER);
                    }
                    if (externalService.getRetryCount() != null) {
                        preparedStatement.setInt(6, externalService.getRetryCount());
                    } else {
                        preparedStatement.setNull(6, java.sql.Types.INTEGER);
                    }
                    Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
                    preparedStatement.setTimestamp(7, currentTimestamp);
                    if (externalService.getCreatedAt() == null) {
                        externalService.setCreatedAt(currentTimestamp.toString());
                    }

                    preparedStatement.executeUpdate();
                }

                insertHeaders(connection, externalService.getId(), externalService.getHeaders());
                connection.commit();
                return externalService;
            } catch (SQLException e) {
                connection.rollback();
                throw new APIMGovernanceException("Error while adding External Service", e);
            }
        } catch (SQLException e) {
            throw new APIMGovernanceException("Failed to get database connection", e);
        }
    }

    @Override
    public ExternalService updateExternalService(ExternalService externalService) throws APIMGovernanceException {
        try (Connection connection = APIMGovernanceDBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_EXTERNAL_SERVICE_SQL)) {
                    preparedStatement.setString(1, externalService.getName());
                    preparedStatement.setString(2, externalService.getUrl());
                    preparedStatement.setString(3, externalService.getPrompt());
                    if (externalService.getTimeoutMs() != null) {
                        preparedStatement.setInt(4, externalService.getTimeoutMs());
                    } else {
                        preparedStatement.setNull(4, java.sql.Types.INTEGER);
                    }
                    if (externalService.getRetryCount() != null) {
                        preparedStatement.setInt(5, externalService.getRetryCount());
                    } else {
                        preparedStatement.setNull(5, java.sql.Types.INTEGER);
                    }
                    preparedStatement.setString(6, externalService.getId());
                    preparedStatement.executeUpdate();
                }

                // Delete existing headers and re-insert
                try (PreparedStatement preparedStatement = connection.prepareStatement(
DELETE_HEADERS_BY_SERVICE_ID_SQL)) {
                    preparedStatement.setString(1, externalService.getId());
                    preparedStatement.executeUpdate();
                }

                insertHeaders(connection, externalService.getId(), externalService.getHeaders());
                connection.commit();
                return getExternalServiceById(externalService.getId());
            } catch (SQLException e) {
                connection.rollback();
                throw new APIMGovernanceException("Error while updating External Service", e);
            }
        } catch (SQLException e) {
            throw new APIMGovernanceException("Failed to get database connection", e);
        }
    }

    @Override
    public void deleteExternalService(String id) throws APIMGovernanceException {
        try (Connection connection = APIMGovernanceDBUtil.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement preparedStatement = connection.prepareStatement(
DELETE_HEADERS_BY_SERVICE_ID_SQL)) {
                    preparedStatement.setString(1, id);
                    preparedStatement.executeUpdate();
                }

                try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_EXTERNAL_SERVICE_SQL)) {
                    preparedStatement.setString(1, id);
                    preparedStatement.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new APIMGovernanceException("Error while deleting External Service", e);
            }
        } catch (SQLException e) {
            throw new APIMGovernanceException("Failed to get database connection", e);
        }
    }

    @Override
    public ExternalService getExternalServiceById(String id) throws APIMGovernanceException {
        ExternalService externalService = null;
        try (Connection connection = APIMGovernanceDBUtil.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(GET_EXTERNAL_SERVICE_BY_ID_SQL)) {
                preparedStatement.setString(1, id);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        externalService = extractExternalService(resultSet);
                    }
                }
            }
            if (externalService != null) {
                List<ExternalServiceHeader> headers = getHeadersForService(connection, id);
                externalService.setHeaders(headers);
            }
        } catch (SQLException e) {
            throw new APIMGovernanceException("Error while retrieving External Service", e);
        }
        return externalService;
    }

    @Override
    public ExternalServiceList getExternalServices() throws APIMGovernanceException {
        ExternalServiceList externalServiceList = new ExternalServiceList();
        List<ExternalService> services = new ArrayList<>();
        try (Connection connection = APIMGovernanceDBUtil.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(GET_ALL_EXTERNAL_SERVICES_SQL);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ExternalService service = extractExternalService(resultSet);
                    service.setHeaders(getHeadersForService(connection, service.getId()));
                    services.add(service);
                }
            }
        } catch (SQLException e) {
            throw new APIMGovernanceException("Error while retrieving all External Services", e);
        }
        externalServiceList.setList(services);
        externalServiceList.setCount(services.size());
        return externalServiceList;
    }

    private void insertHeaders(Connection connection, String serviceId,
                               List<ExternalServiceHeader> headers) throws SQLException {
        if (headers != null && !headers.isEmpty()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    ADD_EXTERNAL_SERVICE_HEADER_SQL)) {
                for (ExternalServiceHeader header : headers) {
                    String category = header.getCategory() != null ? header.getCategory().name() : HeaderCategory.STANDARD.name();
                    preparedStatement.setString(1, serviceId);
                    preparedStatement.setString(2, header.getHeaderKey());
                    preparedStatement.setString(3, header.getHeaderValue());
                    preparedStatement.setString(4, category);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }
        }
    }

    private List<ExternalServiceHeader> getHeadersForService(Connection connection, 
                                                             String serviceId) throws SQLException {
        List<ExternalServiceHeader> headers = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                GET_HEADERS_BY_SERVICE_ID_SQL)) {
            preparedStatement.setString(1, serviceId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ExternalServiceHeader header = new ExternalServiceHeader();
                    header.setId(resultSet.getString("ID"));
                    header.setServiceId(resultSet.getString("SERVICE_ID"));
                    header.setHeaderKey(resultSet.getString("HEADER_KEY"));
                    header.setHeaderValue(resultSet.getString("HEADER_VALUE"));
                    String categoryStr = resultSet.getString("CATEGORY");
                    if (categoryStr != null) {
                        header.setCategory(HeaderCategory.valueOf(categoryStr));
                    }
                    headers.add(header);
                }
            }
        }
        return headers;
    }

    private ExternalService extractExternalService(ResultSet resultSet) throws SQLException {
        ExternalService service = new ExternalService();
        service.setId(resultSet.getString("ID"));
        service.setName(resultSet.getString("NAME"));
        service.setUrl(resultSet.getString("URL"));
        service.setPrompt(resultSet.getString("PROMPT"));
        int timeout = resultSet.getInt("TIMEOUT_MS");
        if (!resultSet.wasNull()) {
            service.setTimeoutMs(timeout);
        }
        int retryCount = resultSet.getInt("RETRY_COUNT");
        if (!resultSet.wasNull()) {
            service.setRetryCount(retryCount);
        }
        Timestamp createdAt = resultSet.getTimestamp("CREATED_AT");
        if (createdAt != null) {
            service.setCreatedAt(createdAt.toString());
        }
        return service;
    }
}

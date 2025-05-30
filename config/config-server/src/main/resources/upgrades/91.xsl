<?xml version="1.0"?>
<!--
  ~ Copyright Thoughtworks, Inc.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:uuid="xalan://java.util.UUID" version="1.0">
  <xsl:template match="/cruise/@schemaVersion" priority="2">
    <xsl:attribute name="schemaVersion">91</xsl:attribute>
  </xsl:template>

  <!-- Copy everything -->
  <xsl:template match="@*|node()" priority="1">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:variable name="migratedContent">
    <xsl:for-each select="//security/ldap">
      <xsl:element name="authConfig">
        <xsl:attribute name="id"><xsl:value-of select="uuid:randomUUID()"/></xsl:attribute>
        <xsl:attribute name="pluginId">cd.go.authentication.ldap</xsl:attribute>
        <xsl:element name="property">
          <xsl:element name="key">Url</xsl:element>
          <xsl:element name="value">
            <xsl:value-of select="./@uri"/>
          </xsl:element>
        </xsl:element>
        <xsl:element name="property">
          <xsl:element name="key">ManagerDN</xsl:element>
          <xsl:element name="value">
            <xsl:value-of select="./@managerDn"/>
          </xsl:element>
        </xsl:element>
        <xsl:element name="property">
          <xsl:element name="key">SearchBases</xsl:element>
          <xsl:element name="value">
            <xsl:for-each select="./bases/base">
              <xsl:value-of select="./@value"/>
              <xsl:text>&#xA;</xsl:text>
            </xsl:for-each>
          </xsl:element>
        </xsl:element>
        <xsl:element name="property">
          <xsl:element name="key">UserLoginFilter</xsl:element>
          <xsl:element name="value">
            <xsl:value-of select="./@searchFilter"/>
          </xsl:element>
        </xsl:element>
        <xsl:element name="property">
          <xsl:element name="key">Password</xsl:element>
          <xsl:if test="./@encryptedManagerPassword">
            <xsl:element name="encryptedValue">
              <xsl:value-of select="./@encryptedManagerPassword"/>
            </xsl:element>
          </xsl:if>
          <xsl:if test="./@managerPassword">
            <xsl:element name="value">
              <xsl:value-of select="./@managerPassword"/>
            </xsl:element>
          </xsl:if>
        </xsl:element>
      </xsl:element>
    </xsl:for-each>

  </xsl:variable>

  <xsl:template match="//security/authConfigs" priority="3">
    <authConfigs>
      <xsl:apply-templates select="@*|node()"/>
      <xsl:copy-of select="$migratedContent"/>
    </authConfigs>
  </xsl:template>

  <xsl:template match="security[not(authConfigs)]" priority="3">
    <security>
      <xsl:apply-templates select="@*|node()"/>
      <authConfigs>
        <xsl:copy-of select="$migratedContent"/>
      </authConfigs>
    </security>
  </xsl:template>

  <!-- Remove ldap and passwordFile elements from security -->
  <xsl:template match="//ldap" priority="4"/>

</xsl:stylesheet>
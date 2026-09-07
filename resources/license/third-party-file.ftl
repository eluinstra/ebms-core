<#--
Renders the third-party notice file for this module.

Available context (see the default third-party-file.ftl of the
license-maven-plugin):
- dependencyMap: Map.Entry<Dependency(MavenProject), String[] licenses>
-->
<#function licenseFormat licenses>
    <#assign result = ""/>
    <#list licenses as license>
        <#assign result = result + " (" + license + ")"/>
    </#list>
    <#return result>
</#function>
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + " - " + (p.url!"no url defined") + ")">
    </#if>
</#function>
This file lists the third-party components bundled into this artifact and
the license under which each component is distributed. The full text of each
license is available at the URL referenced below and, where the component
ships one, in the META-INF license and notice files of this archive.

This product includes software developed at The Apache Software Foundation
(http://www.apache.org/) under Apache License, Version 2.0.

<#if dependencyMap?size == 0>
This artifact contains no third-party components.
<#else>
<#list dependencyMap as e>
    <#assign project = e.getKey()/>
    <#assign licenses = e.getValue()/>
${licenseFormat(licenses)} ${artifactFormat(project)}
</#list>
</#if>

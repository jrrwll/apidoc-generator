<#if name??>${nameHeader} ${name}</#if>
<#list groups as group>
    <#list group.functions as function>
        ${functionHeader} ${function.name}<#if function.path??> <#list function.path as path>`${path}`<#sep> </#list></#if><#lt>
        ${inputParamTitle}<#lt>
        <#list function.inputParams as inputParamName, inputParam>
            ${inputParamNameHeader} ${inputParamName}<#lt>
          ```js<#lt>
            ${inputParam.jsonWithComment}<#lt>
          ```<#lt>
        </#list>
        ${outputParamTitle}<#lt>
      ```js<#lt>
        ${function.outputParam.jsonWithComment}<#lt>
      ```<#lt><#sep><#if functionSep??>${functionSep}</#if></#sep><#lt>
    </#list><#sep><#if groupSep??>${groupSep}</#if></#sep><#lt>
</#list>
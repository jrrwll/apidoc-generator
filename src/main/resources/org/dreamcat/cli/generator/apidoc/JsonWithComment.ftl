<#compress>
    <#if name??>${nameHeader} ${name}</#if>
    <#assign seq = 0>
    <#list groups as group>
        <#list group.functions as function>
            <#assign seq = seq + 1>
            ${functionHeader} <#if seqPrefix??>${seqPrefix}${seq} </#if><#if pinFunctionComment && function.comment??>${function.comment}<#else>${function.name}</#if><#if function.path??> <#list function.path as path>`${path}`<#sep> </#list></#if><#lt>
            <#if pinFunctionComment == false && function.comment??>${'>'} ${function.comment}${'\n'}</#if><#lt>
            <#if inputParamTitle??>${inputParamTitle}</#if><#lt>
            <#if function.inputParamsMerged>
                <#assign fields = function.inputParams[0].fields>
              | ${indentName} | ${indentType} | ${indentRequired} | ${indentComment} |<#lt>
              | :- | :- | :- | :- |<#lt>
                <#include "fields">
            <#else>
                <#list function.inputParams as inputParam>
                    <#if function.inputParamCount gt 1>***${inputParam.name}***<#if inputParam.comment??> ${inputParam.comment}</#if></#if><#lt>
                  ```js<#lt>
                    ${inputParam.jsonWithComment}<#lt><#if function.inputParamCount == 1 && inputParam.comment??> // ${inputParam.comment}</#if><#lt>
                  ```<#lt>
                </#list>
            </#if>
            <#assign outputParam = function.outputParam>
            <#if outputParamTitle??>${outputParamTitle}</#if><#lt>
          ```js<#lt>
            ${outputParam.jsonWithComment}<#if outputParam.comment??> // ${outputParam.comment}</#if><#lt>
          ```<#lt>
        </#list><#lt>
    </#list>
</#compress>
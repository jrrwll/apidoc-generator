<#compress>
    <#if name??>### ${name}</#if><#lt>
</#compress>
<#assign seq = 0>
<#list groups as group>
    <#list group.functions as function>
        <#assign seq = seq + 1>
        #### <#if seqPrefix??>${seqPrefix}${seq} </#if><#if function.path??> <#list function.path as path>`${path}`<#sep> </#list></#if><#lt>
        <#compress>
            <#if function.comment??>${'>'} ${function.comment}${'\n'}</#if><#lt>
        </#compress>

        - *Input Params*<#lt>

        <#if function.inputParamsMerged>
            <#assign fields = function.inputParams[0].fields>
            | Field | Type | Required | Comment |<#lt>
            | :- | :- | :- | :- |<#lt>
            <#include "fields">
        <#else>
            <#list function.inputParams as inputParam>
                <#if function.inputParamCount gt 1>**${inputParam.name}**<#if inputParam.comment??> ${inputParam.comment}</#if>${'\n'}</#if><#lt>
                ```js<#lt>
                  ${inputParam.jsonWithComment!}<#lt><#if function.inputParamCount == 1 && inputParam.comment??> // ${inputParam.comment}</#if><#lt>
                ```<#lt>
            </#list>
        </#if>
        <#assign outputParam = function.outputParam>

        - *Output Params*<#lt>

        ```js<#lt>
          ${outputParam.jsonWithComment!}<#if outputParam.comment??> // ${outputParam.comment}</#if><#lt>
        ```<#lt>

    </#list>
</#list>
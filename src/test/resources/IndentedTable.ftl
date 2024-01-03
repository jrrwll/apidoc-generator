<#assign maxNestLevel = 4>
<#if name??>### ${name}</#if>
<#list groups as group>
    <#list group.functions as function>
        #### ${function.name}<#if function.path??> <#list function.path as path>`${path}`<#sep> </#list></#if><#lt>

        - *Input Params*<#lt>

        | Field | Type | Required | Comment |<#lt>
        | :- | :- | :- | :- |<#lt>
        <#list function.inputParams as inputParam>
            | ${inputParam.name} | ${inputParam.typeName} | <#if inputParam.required?? && inputParam.required == true>Y<#else>N</#if> | ${inputParam.comment} |<#lt>
            <#if maxNestLevel gte 1 && inputParam.fields??>
                <#assign fields = inputParam.fields>
                <#include "fields">
            </#if>
        </#list>

        - *Output Params*<#lt>

        <#if maxNestLevel gte 1 && function.outputParam.fields??>
            <#assign fields = function.outputParam.fields>
            | Field | Type | Required | Comment |<#lt>
            | :- | :- | :- | :- |<#lt>
              <#include "fields">
        </#if>

    </#list>
</#list>
<#if name??>${nameHeader} ${name}</#if>
<#list groups as group>
    <#list group.functions as function>
        ${functionHeader} ${function.name}<#if function.path??> <#list function.path as path>`${path}`<#sep> </#list></#if><#lt>
        <#if paramHeader??>${paramHeader} </#if>${inputParamTitle}<#lt>
      | ${indentName} | ${indentType} | ${indentRequired} | ${indentComment} |<#lt>
      | :- | :- | :- | :- |<#lt>
        <#list function.inputParams as inputParam>
          | ${inputParam.name} | ${inputParam.typeName} | <#if inputParam.required?? && inputParam.required == true>${requiredTrue}<#else>${requiredFalse}</#if> | ${inputParam.comment} |<#lt>
            <#if maxNestLevel gte 1 && inputParam.fields??>
                <#assign fields = inputParam.fields>
                <#include "fields">
            </#if>
        </#list>
        <#if paramHeader??>${paramHeader} </#if>${outputParamTitle}<#lt>
        <#if maxNestLevel gte 1 && function.outputParam.fields??>
            <#assign fields = function.outputParam.fields>
          | ${indentName} | ${indentType} | ${indentRequired} | ${indentComment} |<#lt>
          | :- | :- | :- | :- |<#lt>
            <#include "fields">
        </#if>
    </#list><#sep><#if groupSep??>${groupSep}</#if></#sep><#lt>
</#list>
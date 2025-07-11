<#assign indentPrefix = ' └'>
<#list fields as paramField1>
  |&nbsp;&nbsp;${indentPrefix} ${paramField1.name} | ${paramField1.typeName} | <#if paramField1.required??><#if paramField1.required>Y<#else>N</#if></#if> | ${paramField1.comment} |<#lt>
    <#if maxNestLevel gte 2 && paramField1.fields??>
        <#list paramField1.fields as paramField2>
          |&nbsp;&nbsp;&nbsp;&nbsp;${indentPrefix} ${paramField2.name} | ${paramField2.typeName} | <#if paramField2.required??><#if paramField2.required>Y<#else>N</#if></#if> | ${paramField2.comment} |<#lt>
            <#if maxNestLevel gte 3 && paramField2.fields??>
                <#list paramField2.fields as paramField3>
                  |&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${indentPrefix} ${paramField3.name} | ${paramField3.typeName} | <#if paramField3.required??><#if paramField3.required>Y<#else>N</#if></#if> | ${paramField3.comment} |<#lt>
                    <#if maxNestLevel gte 4 && paramField3.fields??>
                        <#list paramField3.fields as paramField4>
                          |&nbsp;&nbsp;;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${indentPrefix} ${paramField4.name} | ${paramField4.typeName} | <#if paramField4.required??><#if paramField4.required>Y<#else>N</#if></#if> | ${paramField4.comment} |<#lt>
                            <#if maxNestLevel gte 5 && paramField4.fields??>
                                <#list paramField4.fields as paramField5>
                                  |&nbsp;&nbsp;;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${indentPrefix} ${paramField5.name} | ${paramField5.typeName} | <#if paramField5.required??><#if paramField5.required>Y<#else>N</#if></#if> | ${paramField5.comment} |<#lt>
                                    <#if maxNestLevel gte 6 && paramField5.fields??>
                                        <#list paramField5.fields as paramField6>
                                          |&nbsp;&nbsp;;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${indentPrefix} ${paramField6.name} | ${paramField6.typeName} | <#if paramField6.required??><#if paramField6.required>Y<#else>N</#if></#if> | ${paramField6.comment} |<#lt>
                                            <#if paramField6.fields??>
                                                <#list paramField6.fields as paramField7>
                                                  |&nbsp;&nbsp;;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;${indentPrefix} ${paramField7.name} | ${paramField7.typeName} | <#if paramField7.required??><#if paramField7.required>Y<#else>N</#if></#if> | ${paramField7.comment} |<#lt>
                                                </#list>
                                            </#if>
                                        </#list>
                                    </#if>
                                </#list>
                            </#if>
                        </#list>
                    </#if>
                </#list>
            </#if>
        </#list>
    </#if>
</#list>
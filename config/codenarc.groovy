// Copyright 2026 Hamid Gholami
// SPDX-License-Identifier: Apache-2.0

ruleset {
    description('Typed Groovy and focused correctness checks for a Jenkins shared library')
    ruleset('rulesets/basic.xml')
    ruleset('rulesets/imports.xml') {
        NoWildcardImports(enabled: false)
        MisorderedStaticImports(enabled: false)
    }
    FieldTypeRequired()
    MethodParameterTypeRequired()
    MethodReturnTypeRequired()
    VariableTypeRequired()
    ImplicitClosureParameter()
    ImplicitReturnStatement()
    ParameterReassignment()
    SpaceAfterComma()
    SpaceAfterSemicolon()
    SpaceAroundOperator()
    TrailingWhitespace()
    IllegalRegex(regex: /\bdef\b/, violationMessage: 'Use an explicit type instead of def')
    IllegalClassReference(classNames: 'groovy.json.JsonSlurper,groovy.json.JsonSlurperClassic,groovy.util.XmlSlurper')
}

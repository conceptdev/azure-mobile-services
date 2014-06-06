/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Apache 2.0 License
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.windowsazure.mobileservices.table.query;

import java.util.Date;
import java.util.List;

import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.table.serialization.DateSerializer;

/**
 * Query node visitor used to generate SQL filter.
 */
public class QueryNodeSQLWriter implements QueryNodeVisitor<QueryNode> {

	StringBuilder mBuilder;

	public QueryNodeSQLWriter() {
		this.mBuilder = new StringBuilder();
	}

	public StringBuilder getBuilder() {
		return this.mBuilder;
	}

	@Override
	public QueryNode visit(ConstantNode node) {
		Object value = node.getValue();
		String constant = value != null ? value.toString() : "NULL";

		if (value instanceof String) {
			constant = process((String) value);
		} else if (value instanceof Date) {
			constant = process((Date) value);
		} else if (value instanceof Boolean) {
			constant = process((Boolean) value);
		}

		this.mBuilder.append(constant);

		return node;
	}

	@Override
	public QueryNode visit(FieldNode node) {
		this.mBuilder.append(node.getFieldName());

		return node;
	}

	@Override
	public QueryNode visit(UnaryOperatorNode node) throws MobileServiceException {
		if (node.getUnaryOperatorKind() == UnaryOperatorKind.Parenthesis) {
			this.mBuilder.append("(");

			if (node.getArgument() != null) {
				node.getArgument().accept(this);
			}

			this.mBuilder.append(")");
		} else {
			this.mBuilder.append(getSQLOperator(node));

			if (node.getArgument() != null) {
				this.mBuilder.append(" ");
				node.getArgument().accept(this);
			}
		}

		return node;
	}

	@Override
	public QueryNode visit(BinaryOperatorNode node) throws MobileServiceException {
		if (node.getLeftArgument() != null) {
			node.getLeftArgument().accept(this);
			this.mBuilder.append(" ");
		}

		this.mBuilder.append(getSQLOperator(node));

		if (node.getRightArgument() != null) {
			this.mBuilder.append(" ");
			node.getRightArgument().accept(this);
		}

		return node;
	}

	@Override
	public QueryNode visit(FunctionCallNode node) throws MobileServiceException {
		String format = getSQLOperatorFormat(node);

		Object[] args = new Object[node.getArguments().size()];
		List<QueryNode> arguments = node.getArguments();

		for (int index = 0; index < arguments.size(); index++) {
			QueryNode argument = arguments.get(index);
			args[index] = argument.accept(this);
		}

		this.mBuilder.append(String.format(format, args));

		return node;
	}

	private static String getSQLOperator(UnaryOperatorNode node) {
		UnaryOperatorKind operatorKind = node.getUnaryOperatorKind();

		String sqlOperator = "";

		switch (operatorKind) {
		case Not:
			sqlOperator = "NOT";
			break;
		case Parenthesis:
			break;
		}

		return sqlOperator;
	}

	private static String getSQLOperator(BinaryOperatorNode node) {
		BinaryOperatorKind operatorKind = node.getBinaryOperatorKind();

		String sqlOperator = "";

		switch (operatorKind) {
		case Or:
			sqlOperator = "OR";
			break;
		case And:
			sqlOperator = "AND";
			break;
		case Eq:
			sqlOperator = "=";
			if (node.getRightArgument() instanceof ConstantNode) {
				ConstantNode rightArgument = (ConstantNode) node.getRightArgument();
				if (rightArgument.getValue() == null) {
					sqlOperator = "IS";
				}
			}
			break;
		case Ne:
			sqlOperator = "<>";
			if (node.getRightArgument() instanceof ConstantNode) {
				ConstantNode rightArgument = (ConstantNode) node.getRightArgument();
				if (rightArgument.getValue() == null) {
					sqlOperator = "IS NOT";
				}
			}
			break;
		case Gt:
			sqlOperator = ">";
			break;
		case Ge:
			sqlOperator = ">=";
			break;
		case Lt:
			sqlOperator = "<";
			break;
		case Le:
			sqlOperator = "<=";
			break;
		case Add:
			sqlOperator = "+";
			break;
		case Sub:
			sqlOperator = "-";
			break;
		case Mul:
			sqlOperator = "*";
			break;
		case Div:
			sqlOperator = "/";
			break;
		case Mod:
			sqlOperator = "%";
			break;
		}

		return sqlOperator;
	}

	private static String getSQLOperatorFormat(FunctionCallNode node) {
		String operatorFormat = "";
		FunctionCallKind operatorKind = node.getFunctionCallKind();

		switch (operatorKind) {
		case Year:
			operatorFormat = formatDateOperation("%Y");
			break;
		case Month:
			operatorFormat = formatDateOperation("%m");
			break;
		case Day:
			operatorFormat = formatDateOperation("%d");
			break;
		case Hour:
			operatorFormat = formatDateOperation("%H");
			break;
		case Minute:
			operatorFormat = formatDateOperation("%M");
			break;
		case Second:
			operatorFormat = formatDateOperation("%s");
			break;
		case Floor:
			operatorFormat = formatMathOperation(-1);
			break;
		case Ceiling:
			operatorFormat = formatMathOperation(1);
			break;
		case Round:
			operatorFormat = formatMathOperation(0);
			break;
		case ToLower:
			operatorFormat = formatOperation("lower", node.getArguments().size());
			break;
		case ToUpper:
			operatorFormat = formatOperation("upper", node.getArguments().size());
			break;
		case Length:
			operatorFormat = formatOperation("length", node.getArguments().size());
			break;
		case Trim:
			operatorFormat = formatOperation("trim", node.getArguments().size());
			break;
		case StartsWith:
			operatorFormat = formatStartsWithOperation();
			break;
		case EndsWith:
			operatorFormat = formatEndsWithOperation();
			break;
		case SubstringOf:
			operatorFormat = formatSubstringOfOperation();
			break;
		case Concat:
			operatorFormat = formatConcatOperation();
			break;
		case IndexOf:
			operatorFormat = formatIndexOfOperation();
			break;
		case Substring:
			operatorFormat = formatOperation("substring", node.getArguments().size());
			break;
		case Replace:
			operatorFormat = formatOperation("replace", node.getArguments().size());
			break;
		}

		return operatorFormat;
	}

	private static String formatDateOperation(String datePart) {
		StringBuilder builder = new StringBuilder();

		builder.append("CAST(strftime('");
		builder.append(datePart);
		builder.append("', %1$) AS INTEGER)");

		return builder.toString();
	}

	private static String formatMathOperation(Integer roundDirection) {
		StringBuilder builder = new StringBuilder();

		if (roundDirection == 0) {
			builder.append("round(%1$)");
		} else {
			String incorrectInequality = roundDirection < 0 ? ">" : "<";
			String correction = roundDirection < 0 ? "-" : "+";

			builder.append("CASE WHEN round(%1$) ");
			builder.append(incorrectInequality);
			builder.append(" %1$ THEN round(%1$) ");
			builder.append(correction);
			builder.append(" 1 ELSE round(%1$) END");
		}

		return builder.toString();
	}

	private static String formatStartsWithOperation() {
		StringBuilder builder = new StringBuilder();

		builder.append("(instr(%1$,%2$) = 1)");

		return builder.toString();
	}

	private static String formatEndsWithOperation() {
		StringBuilder builder = new StringBuilder();

		builder.append("(instr(%1$,%2$) = (length(%1$) - length(%2$) + 1))");

		return builder.toString();
	}

	private static String formatSubstringOfOperation() {
		StringBuilder builder = new StringBuilder();

		builder.append("(instr(%1$,%2$) >= 1)");

		return builder.toString();
	}

	private static String formatConcatOperation() {
		return "(%1$ || %2$)";
	}

	private static String formatIndexOfOperation() {
		StringBuilder builder = new StringBuilder();

		builder.append("(instr(%1$,%2$) - 1)");

		return builder.toString();
	}

	private static String formatOperation(String operation, Integer totalArguments) {
		StringBuilder builder = new StringBuilder();

		builder.append(operation);
		builder.append("(");
		formatArguments(totalArguments);
		builder.append(")");

		return builder.toString();
	}

	private static String formatArguments(Integer totalArguments) {
		StringBuilder builder = new StringBuilder();

		boolean first = true;

		for (int index = 1; index <= totalArguments; index++) {
			if (!first) {
				builder.append(",");
			} else {
				first = false;
			}

			builder.append("%");
			builder.append(index);
			builder.append("$");
		}

		return builder.toString();
	}

	private static String process(String s) {
		return "'" + sanitize(s) + "'";
	}

	private static String process(Date date) {
		return "'" + sanitize(DateSerializer.serialize(date)) + "'";
	}

	private static String process(Boolean value) {
		
		if (value) {
			return "1";
		}
		
		return "0";
	}
	
	/**
	 * Sanitizes the string to use in a oData query
	 * 
	 * @param s
	 *            The string to sanitize
	 * @return The sanitized string
	 */
	private static String sanitize(String s) {
		if (s != null) {
			return s.replace("'", "''");
		} else {
			return null;
		}
	}
}
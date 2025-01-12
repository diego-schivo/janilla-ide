/*
 * MIT License
 *
 * Copyright (c) 2024-2025 Diego Schivo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.janilla.ide;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.stream.Collectors;

public class CssIndent implements Indent {

	private static final String SPACES = "  ";

	@Override
	public String apply(String string) {
		class A {

			boolean comment;

			int indentation;

			Deque<Integer> block = new ArrayDeque<>();

			boolean statement;
		}
		var a = new A();
		var sb = new StringBuilder();
		var ll = string.lines().map(x -> {
//			System.out.println("CssIndent.apply, x=" + x);
			var x2 = x.trim();
			if (x2.isEmpty())
				return x2;
			if (!a.comment && x2.equals("/*"))
				a.comment = true;
			if (!a.comment) {
				if (x2.startsWith("}") && !x2.endsWith("{"))
					a.indentation = a.block.pop();
				sb.repeat(SPACES, a.indentation).append(x2);
				if (!x2.startsWith("}") && !x2.endsWith(";") && !x2.endsWith(",")) {
					if (x2.endsWith("{")) {
						a.block.push(a.statement ? a.indentation - 1 : a.indentation);
						if (a.statement)
							a.statement = false;
						else
							a.indentation++;
					} else {
						a.statement = true;
						a.indentation++;
					}
				} else if (a.statement && x2.endsWith(";")) {
					a.indentation--;
					a.statement = false;
				}
			} else {
				var i = x.length();
				while (i > 0 && x.charAt(i - 1) == ' ')
					i--;
				sb.append(x, 0, i);
				if (x2.equals("*/"))
					a.comment = false;
			}
			var s = sb.toString();
//			System.out.println("CssIndent.apply, s=" + s);
			sb.setLength(0);
			return s;
		}).collect(Collectors.toCollection(ArrayList::new));
		if (!ll.isEmpty() && !ll.get(ll.size() - 1).isEmpty())
			ll.add("");
		return ll.stream().collect(Collectors.joining("\n"));
	}
}

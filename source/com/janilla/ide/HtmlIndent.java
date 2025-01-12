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

import java.util.ArrayList;
import java.util.stream.Collectors;

public class HtmlIndent implements Indent {

	private static final String SPACES = "  ";

	@Override
	public String apply(String string) {
		class A {

			int indentation;
		}
		var a = new A();
		var sb = new StringBuilder();
		var ll = string.lines().map(x -> {
//			System.out.println("HtmlIndent.apply, x=" + x);
			var x2 = x.trim();
			if (x2.isEmpty())
				return x2;
			if (x2.startsWith("</"))
				a.indentation--;
			sb.repeat(SPACES, a.indentation).append(x2);
			if (x2.startsWith("<") && !x2.startsWith("<!") && !x2.contains("</") && !x2.endsWith("/>"))
				a.indentation++;
			var s = sb.toString();
//			System.out.println("HtmlIndent.apply, s=" + s);
			sb.setLength(0);
			return s;
		}).collect(Collectors.toCollection(ArrayList::new));
		if (!ll.isEmpty() && !ll.get(ll.size() - 1).isEmpty())
			ll.add("");
		return ll.stream().collect(Collectors.joining("\n"));
	}
}

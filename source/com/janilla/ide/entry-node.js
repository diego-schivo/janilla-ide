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
import { FlexibleElement } from "./flexible-element.js";

export default class EntryNode extends FlexibleElement {

	static get observedAttributes() {
		return ["data-name", "data-directory", "data-expandable", "data-expanded"];
	}

	static get templateName() {
		return "entry-node";
	}

	get path() {
		let n = this;
		const xx = [];
		do {
			xx.push(n.dataset.name);
			n = n.parentElement.closest("entry-node, entry-tree");
		} while (n.tagName.toLowerCase() !== "entry-tree");
		return xx.reverse().join("/");
	}

	get state() {
		let s = this.closest("entry-tree").state;
		let nn = s.nodes;
		for (const x of this.path.split("/")) {
			s = nn[x];
			nn = s.children;
		}
		return s;
	}

	constructor() {
		super();
	}

	async updateDisplay() {
		// console.log("EntryNode.updateDisplay");
		const s = this.state;
		const p1 = this.path;
		const p2 = this.closest("janilla-ide").state.path;
		this.appendChild(this.interpolateDom({
			$template: "",
			...s,
			content: s.children ? {
				$template: "content",
				items: Object.values(s.children).map(x => ({
					$template: "item",
					...x,
					expanded: !!x.children,
					active: `${p1}/${x.name}` === p2
				}))
			} : null
		}));
	}
}

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

export default class JanillaIde extends FlexibleElement {

	static get templateName() {
		return "janilla-ide";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		// console.log("JanillaIde.connectedCallback");
		// super.connectedCallback();
		addEventListener("popstate", this.handlePopState);
		this.addEventListener("click", this.handleClick);
	}

	disconnectedCallback() {
		// console.log("JanillaIde.disconnectedCallback");
		removeEventListener("popstate", this.handlePopState);
		this.removeEventListener("click", this.handleClick);
	}

	handleClick = async event => {
		// console.log("JanillaIde.handleClick", event);
		if (event.target.tagName.toLowerCase() === "a") {
			event.preventDefault();
			const p = event.target.getAttribute("href");
			this.state.entryTree.path = p;
			this.state.fileEditor = { path: p };
			history.pushState(this.state, "", `/edit/${p}`);
			this.requestUpdate();
		}
	}

	handlePopState = event => {
		// console.log("JanillaIde.handlePopState", event);
		this.state = history.state;
		this.requestUpdate();
		// this.querySelector("entry-tree")?.requestUpdate();
	}

	async updateDisplay() {
		// console.log("JanillaIde.updateDisplay");
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.state
		}));
	}
}

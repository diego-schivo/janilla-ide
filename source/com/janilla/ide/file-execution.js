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
import { UpdatableHTMLElement } from "./updatable-html-element.js";

export default class FileExecution extends UpdatableHTMLElement {

	static get templateName() {
		return "file-execution";
	}

	constructor() {
		super();
	}

	connectedCallback() {
		super.connectedCallback();
		this.addEventListener("submit", this.handleSubmit);
	}

	disconnectedCallback() {
		super.disconnectedCallback();
		this.removeEventListener("submit", this.handleSubmit);
	}

	handleSubmit = async event => {
		event.preventDefault();
		const p = this.closest("editor-list").dataset.path;
		switch (event.submitter.name) {
			case "run":
				this.state.output = "";
				await (await fetch(`/api/executions/${p}`, { method: "POST" })).json();
				const es = new EventSource(`/api/executions/${p}`);
				es.onopen = () => {
					//console.log("Connection to server opened.");
				};
				es.onmessage = async e => {
					//console.log(e.data);
					if (e.data === "ok") {
						es.close();
						return;
					}
					this.state.output += e.data + "\n";
					this.requestUpdate();
				};
				es.onerror = () => {
					//console.log("EventSource failed.");
				};
				break;
			case "terminate":
				await (await fetch(`/api/executions/${p}`, { method: "DELETE" })).json();
				break;
		}
	}

	async updateDisplay() {
		this.appendChild(this.interpolateDom({
			$template: "",
			...this.state
		}));
	}
}

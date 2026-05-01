---
name: multi-agent-orchestrator
description: Use this when the user wants Codex to run this repository's local multi-agent orchestration system for code review, fixing, test planning, explaining code, or a chained review, fix, test, and summary workflow. Trigger when the user mentions multi-agent collaboration, asks to use the local orchestrator, asks for review/fix/test workflow automation, or explicitly invokes $multi-agent-orchestrator.
---

# Multi Agent Orchestrator

Use the local Node.js orchestrator in this repository instead of manually simulating the workflow.

## Quick Use

Run:

```powershell
npm --prefix multi-agent-orchestrator run chat -- "<user request>"
```

Examples:

```powershell
npm --prefix multi-agent-orchestrator run chat -- "explain this function"
npm --prefix multi-agent-orchestrator run chat -- "review this file, fix the issues, and suggest tests"
npm --prefix multi-agent-orchestrator run chat -- "continue from the previous review result and fix it"
```

## Behavior

- Single-intent requests route to one agent: `review`, `fix`, `test`, or `explain`.
- Multi-step requests route to workflow mode: `review -> fix -> test -> summary`.
- Historical references such as "previous result", "last step", and "continue" use `.agent-session.json`.
- If no `AGENT_LLM_ENDPOINT` is configured, the orchestrator uses local rule routing and simulated agent output.

## When The User Wants Real Model Output

Check `multi-agent-orchestrator/config.json`. If `llm.provider` is `mock`, explain that the current run validates orchestration only, not real code intelligence.

Prefer Codex CLI when the user asks to use Codex directly:

```powershell
npm --prefix multi-agent-orchestrator run chat -- "review this file, fix the issues, and suggest tests"
```

Set `llm.provider` to `codex` and `llm.codex.model` to the desired model in `multi-agent-orchestrator/config.json`.

The endpoint is expected to accept:

```json
{
  "system": "Agent prompt",
  "user": "Agent input",
  "tools": []
}
```

And return one of:

```json
{
  "output_text": "model output"
}
```

## Validation

After changing orchestrator code, run:

```powershell
npm --prefix multi-agent-orchestrator run check
```

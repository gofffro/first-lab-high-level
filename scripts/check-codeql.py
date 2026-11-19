import json
import sys
from pathlib import Path


def main(directory):
    reports = sorted(Path(directory).rglob("*.sarif"))
    if not reports:
        raise ValueError("No CodeQL report was produced")

    errors = 0
    for report in reports:
        data = json.loads(report.read_text(encoding="utf-8"))
        if not data.get("runs"):
            raise ValueError(f"No analysis runs in {report.name}")
        for run in data["runs"]:
            driver = run["tool"]["driver"]
            if "codeql" not in driver["name"].lower():
                raise ValueError("Expected a CodeQL report")
            if any(item.get("executionSuccessful") is False for item in run.get("invocations", [])):
                raise ValueError("CodeQL reported a failed analysis")
            rules = {rule["id"]: rule for rule in driver.get("rules", [])}
            for result in run["results"]:
                rule = rules.get(result.get("ruleId"), {})
                level = result.get("level", rule.get("defaultConfiguration", {}).get("level", "warning"))
                print(f"{level}: {result.get('ruleId', 'unknown')}")
                if level == "error":
                    errors += 1
    if errors:
        raise ValueError(f"CodeQL reported {errors} error-level findings")
    print(f"CodeQL passed ({len(reports)} reports, no error-level findings)")


if __name__ == "__main__":
    try:
        main(sys.argv[1])
    except (ValueError, OSError, KeyError, TypeError) as error:
        sys.exit(str(error))

from typing import Collection, List


class Key:
    def __init__(self, key: Collection[str]):
        self._attrs = frozenset(key)

    def getAttributes(self) -> List[str]:
        return list(self._attrs)

    def setAttributes(self, newKeyAttrs: List[str]) -> None:
        self._attrs = frozenset(newKeyAttrs)

    def size(self) -> int:
        return len(self._attrs)

    def contains(self, key: "Key") -> bool:
        return self._attrs.issuperset(key._attrs)

    def __hash__(self) -> int:
        return hash(self._attrs)

    def __eq__(self, other: object) -> bool:
        if not isinstance(other, Key):
            return False
        return self._attrs == other._attrs

    def __str__(self) -> str:
        return str(sorted(self._attrs))

    def __repr__(self) -> str:
        return f"Key({sorted(self._attrs)!r})"